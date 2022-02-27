package org.simplebackup.simplebackup.service;

import io.github.abarhub.vfs.core.api.VFS4JFiles;
import io.github.abarhub.vfs.core.api.path.VFS4JPathName;
import org.simplebackup.simplebackup.runner.Runner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.stream.Stream;

@Service
public class BackupService {

    private static final Logger LOGGER = LoggerFactory.getLogger(Runner.class);

    public void backup(VFS4JPathName src, VFS4JPathName dest) throws IOException {
        LOGGER.info("backup '{}' -> '{}'", src, dest);


        try (var directoryStream = VFS4JFiles.newDirectoryStream(src, (x)-> true)) {
            for (VFS4JPathName file : directoryStream) {
                //Path p = directorySource.relativize(file);
                //Path res = directoryCible.resolve(p);
                String p=src.relativize(file.getPath());
                var filenameSrc=src.getFilename();
                VFS4JPathName res;
                if(StringUtils.hasText(filenameSrc)){
                    res=dest.resolve(filenameSrc).resolve(p);
                } else {
                    res=dest.resolve(p);
                }
                LOGGER.info("file: {}", file);
                LOGGER.info("file2: {}", file.normalize());
                LOGGER.info("relativise: {}", p);
                LOGGER.info("cible: {}", res);

                if(VFS4JFiles.isDirectory(file)) {
                    //try (Stream<Path> listeFile = Files.list(file)) {
                    try (Stream<VFS4JPathName> listeFile = VFS4JFiles.list(file)) {
                        listeFile.forEach(p2 -> {
                            //Path p3;
                            VFS4JPathName p3;
                            String filename = p2.getName(p2.getNameCount() - 1);
                            LOGGER.info("filename: {}", filename);
                            //p3 = res.resolve(p2.getFileName() + ".crp");
                            p3 = res.resolve(filename );
                            LOGGER.info("fichier src: {}", p2);
                            LOGGER.info("fichier cible: {}", p3);
                            try {
                                VFS4JFiles.createDirectories(p3.getParent());
                                //Files.createDirectories(p3.getParent());
                                //if (!Files.exists(p3)) {
                                if (!VFS4JFiles.exists(p3)) {
                                    //copyFile(p2, p3, true, password);
                                    VFS4JFiles.copy(p2, p3);
                                    LOGGER.info("copie du fichier '{}' OK", p3);
                                }
                                //Path p3Hash = p3.getParent().resolve(p3.getFileName() + ".sha256");
//                            VFS4JPathName p3Hash=p3.getParent().resolve(p3.getName(p3.getNameCount()-1)+ ".sha256");
//                        verifieHash(p3, p3Hash);
                                //Path p2Hash = p3.getParent().resolve(p2.getFileName() + ".sha256");
                                //Path p2Hash = p3.getParent().resolve(filename + ".sha256");
//                            VFS4JPathName p2Hash = p3.getParent().resolve(filename + ".sha256");
//                        verifieHash(p2, p2Hash);
//                        construitListeFichiers(p2, p3.getParent(), fileManager);
                            } catch (IOException e) {
                                throw new UncheckedIOException("Erreur pour copier le fichier " + p3, e);
                            } catch (Exception e) {
                                throw new RuntimeException("Erreur pour copier le fichier " + p3, e);
                            }
                        });
                    }
                } else {
                    VFS4JPathName p3;
//                    String filename = file.getName(file.getNameCount() - 1);
//                    LOGGER.info("filename: {}", filename);
                    //p3 = res.resolve(p2.getFileName() + ".crp");
//                    p3 = res.resolve(filename );
                    LOGGER.info("fichier src: {}", file);
                    LOGGER.info("fichier cible: {}", res);
                    if (!VFS4JFiles.exists(res)) {
                        //copyFile(p2, p3, true, password);
                        if(!VFS4JFiles.exists(res.getParent())) {
                            VFS4JFiles.createDirectories(res.getParent());
                        }
                        VFS4JFiles.copy(file, res);
                        LOGGER.info("copie du fichier '{}' OK", res);
                    }
                }
            }
        }


        LOGGER.info("backup ok");
    }


}
