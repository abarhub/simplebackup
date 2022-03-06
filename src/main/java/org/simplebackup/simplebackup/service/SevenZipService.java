package org.simplebackup.simplebackup.service;

import io.github.abarhub.vfs.core.api.VFS4JDefaultFileManager;
import io.github.abarhub.vfs.core.api.path.VFS4JPathName;
import org.simplebackup.simplebackup.utils.StreamGobbler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.annotation.PreDestroy;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

@Service
public class SevenZipService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SevenZipService.class);

    @Value("${path-sevenzip:}")
    private String pathSevenZip;

    @Value("${volume-size:}")
    private String volumeSize;

    private ExecutorService executorService = Executors.newCachedThreadPool();

    @PreDestroy
    public void terminate() {
        LOGGER.info("fin du executorService");
        executorService.shutdown();
        LOGGER.info("fin du executorService OK");
    }

    public void compress(VFS4JPathName source, VFS4JPathName destination, List<String> exclude,
                         boolean crypt, String password) throws IOException, InterruptedException {

        run(source, destination, exclude, crypt, password);
    }

    private void run(VFS4JPathName source, VFS4JPathName destination, List<String> exclude,
                     boolean crypt, String password) throws IOException, InterruptedException {

        if (!StringUtils.hasText(pathSevenZip)) {
            throw new IllegalArgumentException("config 'path-sevenzip' empty");
        }

        var filemanager = VFS4JDefaultFileManager.get();
        Path sourceFile = filemanager.getRealFile(source);
        Path destinationFile = filemanager.getRealFile(destination);

        var dest = compression(sourceFile, destinationFile, exclude, crypt, password);

        verifieFichier(dest, crypt, password);
    }

    private Path compression(Path sourceFile, Path destinationFile, List<String> exclude,
                             boolean crypt, String password) throws IOException, InterruptedException {

        final List<String> listeResultat = new Vector<>();
        Consumer<String> consumer = (x) -> {
            LOGGER.info("stdout compress: {}", x);
            listeResultat.add(x);
        };
        List<String> listParameter = new ArrayList<>();
        listParameter.add(pathSevenZip);
        listParameter.add("a");
        if (!CollectionUtils.isEmpty(exclude)) {
            for (var ex : exclude) {
                if (StringUtils.hasText(ex)) {
                    listParameter.add("-xr!" + ex);
                }
            }
        }
        if (crypt) {
            if (StringUtils.hasText(password)) {
                listParameter.add("-p" + password);
            } else {
                throw new IllegalArgumentException("Empty password");
            }
        }
        if (StringUtils.hasText(volumeSize)) {
            listParameter.add("-v" + volumeSize);
        }
        listParameter.add(destinationFile.toString());
        listParameter.add(sourceFile.toString());
        int res = runCommand(consumer, listParameter.toArray(new String[0]));
        LOGGER.info("res exec: {}", res);
        if (res != 0) {
            LOGGER.error("Erreur pour compresser {}", sourceFile);
            throw new IOException("Erreur pour créer le fichier " + destinationFile);
        } else {
            if (!Files.exists(destinationFile)) {
                var tmp = destinationFile.getParent().resolve(destinationFile.getFileName().toString() + ".001");
                if (Files.exists(tmp)) {
                    destinationFile = tmp;
                } else {
                    throw new IOException("Erreur pour la création du fichier : " + destinationFile);
                }
            }
            LOGGER.info("Fichier {} créé", destinationFile);
        }
        return destinationFile;
    }

    private void verifieFichier(Path destinationFile, boolean crypt, String password) throws IOException, InterruptedException {
        if (destinationFile == null) {
            throw new IllegalArgumentException("path null");
        }
        final List<String> listeResultat = new Vector<>();
        Consumer<String> consumer = (x) -> {
            LOGGER.info("stdout verif: {}", x);
            listeResultat.add(x);
        };
        List<String> listParameter = new ArrayList<>();
        listParameter.add(pathSevenZip);
        listParameter.add("t");
        if (crypt) {
            if (StringUtils.hasText(password)) {
                listParameter.add("-p" + password);
            } else {
                throw new IllegalArgumentException("Empty password");
            }
        }
        listParameter.add(destinationFile.toString());
        int res = runCommand(consumer, listParameter.toArray(new String[0]));
        LOGGER.info("res exec: {}", res);
        if (res != 0) {
            LOGGER.error("Erreur pour fichier le fichier {}", destinationFile);
            throw new IOException("Erreur pour tester le fichier " + destinationFile);
        } else {
            //VFS4JFiles.write(f, listeResultat, StandardCharsets.UTF_8);
            LOGGER.info("Fichier {} testé", destinationFile);
        }
    }

    private int runCommand(Consumer<String> consumer, String... commandes) throws InterruptedException, IOException {
        ProcessBuilder builder = new ProcessBuilder();
        List<String> liste = new ArrayList<>();
        List<String> listeShow = new ArrayList<>();
        for (String s : commandes) {
            var s2 = s;
            if (s.contains(" ")) {
                s2 = "\"" + s + "\"";
            }
            liste.add(s2);
            if (s.startsWith("-p")) {
                listeShow.add("-pXXX");
            } else {
                listeShow.add(s2);
            }
        }
        LOGGER.info("run {}", listeShow);
        LOGGER.trace("run {}", liste);
        builder.command(liste);
        Process process = builder.start();
        StreamGobbler streamGobbler =
                new StreamGobbler(process.getInputStream(), consumer::accept);
        executorService.submit(streamGobbler);
        StreamGobbler streamGobblerErrur =
                new StreamGobbler(process.getErrorStream(), (x) -> {
                    LOGGER.error("error: {}", x);
                });
        executorService.submit(streamGobblerErrur);
        LOGGER.info("run ...");
        var res = process.waitFor();
        LOGGER.info("run end");
        return res;
    }

}
