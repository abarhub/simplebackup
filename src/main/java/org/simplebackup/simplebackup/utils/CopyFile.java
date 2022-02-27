package org.simplebackup.simplebackup.utils;

import io.github.abarhub.vfs.core.api.*;
import io.github.abarhub.vfs.core.api.path.VFS4JPathName;
import io.github.abarhub.vfs.core.api.path.VFS4JPaths;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class CopyFile {

    private static final Logger LOGGER = LoggerFactory.getLogger(CopyFile.class);

    private String chemin7zip;

    public CopyFile(String chemin7zip) {
        this.chemin7zip = chemin7zip;
    }

    public void copyFile(VFS4JPathName fileSource, VFS4JPathName fileDestination, boolean crypt, String password) throws Exception {

        if (!VFS4JFiles.exists(fileSource)) {
            throw new IllegalArgumentException("Le fichier source '" + fileSource + "' n'existe pas");
        }
        if (VFS4JFiles.exists(fileDestination)) {
            throw new IllegalArgumentException("Le fichier cible '" + fileDestination + "' existe");
        }

        AESCrypt aes = new AESCrypt(true, password);

        if (crypt) {
            aes.encrypt(2, fileSource, fileDestination);
        } else {
            aes.decrypt(fileSource, fileDestination);
        }
    }

    public boolean compare(Path file, Path file2) throws IOException {

        if (!Files.exists(file)) {
            throw new IllegalArgumentException("Le fichier '" + file + "' n'existe pas");
        }
        if (!Files.exists(file2)) {
            throw new IllegalArgumentException("Le fichier 2 '" + file2 + "' n'existe pas");
        }

        return FileUtils.contentEquals(file.toFile(), file2.toFile());
    }

    public void copyDirectory(Path directorySource, Path directoryCible, String password) throws IOException {

        if (!Files.exists(directorySource)) {
            throw new IllegalArgumentException("Le fichier '" + directorySource + "' n'existe pas");
        }
        if (!Files.exists(directoryCible)) {
            throw new IllegalArgumentException("Le fichier 2 '" + directoryCible + "' n'existe pas");
        }

        LOGGER.info("directorySource: {}", directorySource);
        LOGGER.info("directoryCible: {}", directoryCible);


        Properties properties = new Properties();

        properties.setProperty("vfs.paths.src.path", directorySource.toString());
        properties.setProperty("vfs.paths.src.readonly", "true");
        properties.setProperty("vfs.paths.dest.path", directoryCible.toString());
        properties.setProperty("vfs.paths.dest.readonly", "false");

        VFS4JParseConfigFile parseConfigFile = new VFS4JParseConfigFile();
        VFS4JFileManagerBuilder fileManagerBuilder = parseConfigFile.parse(properties);
        VFS4JDefaultFileManager.get().setConfig(fileManagerBuilder.build());
        VFS4JFiles.reinit();
        VFS4JFileManager fileManager=VFS4JDefaultFileManager.get();

        VFS4JPathName src= VFS4JPaths.get("src","");
        VFS4JPathName dest=VFS4JPaths.get("dest","");
        //try (var directoryStream = Files.newDirectoryStream(directorySource)) {
        try (var directoryStream = VFS4JFiles.newDirectoryStream(src, (x)-> true)) {
            for (VFS4JPathName file : directoryStream) {
                //Path p = directorySource.relativize(file);
                //Path res = directoryCible.resolve(p);
                String p=src.relativize(file.getPath());
                VFS4JPathName res=dest.resolve(p);
                LOGGER.info("file: {}", file);
                LOGGER.info("file2: {}", file.normalize());
                LOGGER.info("relativise: {}", p);
                LOGGER.info("cible: {}", res);

                //try (Stream<Path> listeFile = Files.list(file)) {
                try (Stream<VFS4JPathName> listeFile = VFS4JFiles.list(file)) {
                    listeFile.forEach(p2 -> {
                        //Path p3;
                        VFS4JPathName p3;
                        String filename=p2.getName(p2.getNameCount()-1);
                        LOGGER.info("filename: {}", filename);
                        //p3 = res.resolve(p2.getFileName() + ".crp");
                        p3 = res.resolve(filename + ".crp");
                        LOGGER.info("fichier src: {}", p2);
                        LOGGER.info("fichier cible: {}", p3);
                        try {
                            VFS4JFiles.createDirectories(p3.getParent());
                            //Files.createDirectories(p3.getParent());
                            //if (!Files.exists(p3)) {
                            if(!VFS4JFiles.exists(p3)){
                                copyFile(p2, p3, true, password);
                                LOGGER.info("copie du fichier '{}' OK", p3);
                            }
                            //Path p3Hash = p3.getParent().resolve(p3.getFileName() + ".sha256");
                            VFS4JPathName p3Hash=p3.getParent().resolve(p3.getName(p3.getNameCount()-1)+ ".sha256");
                            verifieHash(p3, p3Hash);
                            //Path p2Hash = p3.getParent().resolve(p2.getFileName() + ".sha256");
                            //Path p2Hash = p3.getParent().resolve(filename + ".sha256");
                            VFS4JPathName p2Hash = p3.getParent().resolve(filename + ".sha256");
                            verifieHash(p2, p2Hash);
                            construitListeFichiers(p2, p3.getParent(), fileManager);
                        } catch (IOException e) {
                            throw new UncheckedIOException("Erreur pour copier le fichier " + p3, e);
                        } catch (Exception e) {
                            throw new RuntimeException("Erreur pour copier le fichier " + p3, e);
                        }
                    });
                }
            }
        }
    }

    private void construitListeFichiers(Path fichierSource, Path repertoire) throws IOException, InterruptedException {
        String path7z = chemin7zip;
        if (fichierSource.getFileName().toString().endsWith(".7z")
                || fichierSource.getFileName().toString().endsWith(".7z.001")) {
            Path f = repertoire.resolve(fichierSource.getFileName() + ".lst");
            if (!Files.exists(f)) {
                final List<String> listeResultat = new Vector<>();
                Consumer<String> consumer = listeResultat::add;
                int res = runCommand(consumer, path7z, "l", fichierSource.toString());
                LOGGER.info("res exec: {}", res);
                if (res != 0) {
                    LOGGER.error("Erreur pour lister le fichier {}", fichierSource);
                } else {
                    Files.write(f, listeResultat);
                    LOGGER.info("Fichier listing {} créé", f);
                }
            }
        }
    }

    private void construitListeFichiers(VFS4JPathName fichierSource, VFS4JPathName repertoire, VFS4JFileManager fileManager) throws IOException, InterruptedException {
        String path7z = chemin7zip;
        String filename=fichierSource.getName(fichierSource.getNameCount()-1);
        if (filename.endsWith(".7z")
                || filename.endsWith(".7z.001")) {
            VFS4JPathName f = repertoire.resolve(filename + ".lst");
            if (!VFS4JFiles.exists(f)) {
                final List<String> listeResultat = new Vector<>();
                Consumer<String> consumer = listeResultat::add;
                Path p=fileManager.getRealFile(fichierSource);
                int res = runCommand(consumer, path7z, "l", p.toString());
                LOGGER.info("res exec: {}", res);
                if (res != 0) {
                    LOGGER.error("Erreur pour lister le fichier {}", fichierSource);
                } else {
                    VFS4JFiles.write(f, listeResultat, StandardCharsets.UTF_8);
                    LOGGER.info("Fichier listing {} créé", f);
                }
            }
        }
    }

    private int runCommand(Consumer<String> consumer, String... commandes) throws InterruptedException, IOException {
        ProcessBuilder builder = new ProcessBuilder();
        List<String> liste = new ArrayList<>();
        for (String s : commandes) {
            if (s.contains(" ")) {
                liste.add("\"" + s + "\"");
            } else {
                liste.add(s);
            }
        }
        LOGGER.info("run {}", liste);
        builder.command(liste);
        Process process = builder.start();
        ExecutorService executorService = Executors.newCachedThreadPool();
        //LOGGER.info("output: {}",x);
        StreamGobbler streamGobbler =
                new StreamGobbler(process.getInputStream(), consumer::accept);
        executorService.submit(streamGobbler);
        StreamGobbler streamGobblerErrur =
                new StreamGobbler(process.getErrorStream(), (x) -> {
                    LOGGER.error("error: {}", x);
                });
        executorService.submit(streamGobblerErrur);
        return process.waitFor();
    }

    private void verifieHash(Path fichierTeste, Path fichierHash) throws IOException, DecoderException {
        if (!Files.exists(fichierHash)) {
            byte[] sha256digest = DigestUtils.digest(DigestUtils.getSha256Digest(), fichierTeste.toFile());
            List<String> liste = new ArrayList<>();
            liste.add(Hex.encodeHexString(sha256digest));
            Files.write(fichierHash, liste);
            LOGGER.info("hash hash256 '{}' OK", fichierHash);
        } else {
            byte[] sha256digest = DigestUtils.digest(DigestUtils.getSha256Digest(), fichierTeste.toFile());
            List<String> liste = Files.readAllLines(fichierHash);
            if (CollectionUtils.isEmpty(liste)) {
                LOGGER.error("Le fichier hash '{}' est vide", fichierHash);
            } else if (sha256digest == null || sha256digest.length == 0) {
                LOGGER.error("Erreur pour calculer le hash du fichier '{}'", fichierHash);
            } else {
                String hashFichier = liste.get(0);
                if (Arrays.equals(sha256digest, Hex.decodeHex(hashFichier))) {
                    LOGGER.info("Vérification hash hash256 '{}' OK", fichierHash);
                } else {
                    LOGGER.info("Hash hash256 '{}' différent KO", fichierHash);
                }
            }
        }
    }

    private void verifieHash(VFS4JPathName fichierTeste, VFS4JPathName fichierHash) throws IOException, DecoderException {
        if (!VFS4JFiles.exists(fichierHash)) {
            byte[] sha256digest = DigestUtils.digest(DigestUtils.getSha256Digest(), VFS4JFiles.newInputStream(fichierTeste));
            List<String> liste = new ArrayList<>();
            liste.add(Hex.encodeHexString(sha256digest));
            VFS4JFiles.write(fichierHash, liste, StandardCharsets.UTF_8);
            LOGGER.info("hash hash256 '{}' OK", fichierHash);
        } else {
            byte[] sha256digest = DigestUtils.digest(DigestUtils.getSha256Digest(), VFS4JFiles.newInputStream(fichierTeste));
            List<String> liste = VFS4JFiles.readAllLines(fichierHash);
            if (CollectionUtils.isEmpty(liste)) {
                LOGGER.error("Le fichier hash '{}' est vide", fichierHash);
            } else if (sha256digest == null || sha256digest.length == 0) {
                LOGGER.error("Erreur pour calculer le hash du fichier '{}'", fichierHash);
            } else {
                String hashFichier = liste.get(0);
                if (Arrays.equals(sha256digest, Hex.decodeHex(hashFichier))) {
                    LOGGER.info("Vérification hash hash256 '{}' OK", fichierHash);
                } else {
                    LOGGER.info("Hash hash256 '{}' différent KO", fichierHash);
                }
            }
        }
    }

}
