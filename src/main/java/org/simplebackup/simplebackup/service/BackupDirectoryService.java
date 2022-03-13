package org.simplebackup.simplebackup.service;

import io.github.abarhub.vfs.core.api.VFS4JDefaultFileManager;
import io.github.abarhub.vfs.core.api.VFS4JFileManager;
import io.github.abarhub.vfs.core.api.VFS4JFiles;
import io.github.abarhub.vfs.core.api.path.VFS4JPathName;
import org.simplebackup.simplebackup.model.DirectoryToCompress;
import org.simplebackup.simplebackup.utils.GlobUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.stream.Stream;

@Service
public class BackupDirectoryService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BackupDirectoryService.class);

    public void backup(DirectoryToCompress directory) throws IOException {

        var src = directory.pathSource();
        var dest = directory.pathDestination();
        var exclusion = directory.exclude();
        var exclusionList = GlobUtils.getPathMatcherList(exclusion, VFS4JDefaultFileManager.get());

        try (var directoryStream = VFS4JFiles.newDirectoryStream(src, (x) -> !GlobUtils.aExclure(x, exclusionList))) {
            for (VFS4JPathName file : directoryStream) {
                String p = src.relativize(file.getPath());
                var filenameSrc = src.getFilename();
                VFS4JPathName res;
                if (StringUtils.hasText(filenameSrc)) {
                    res = dest.resolve(filenameSrc).resolve(p);
                } else {
                    res = dest.resolve(p);
                }
                LOGGER.info("file: {}", file);
                LOGGER.info("file2: {}", file.normalize());
                LOGGER.info("relativise: {}", p);
                LOGGER.info("cible: {}", res);

                if (VFS4JFiles.isDirectory(file)) {
                    try (Stream<VFS4JPathName> listeFile = VFS4JFiles.list(file)) {
                        listeFile.forEach(p2 -> {
                            VFS4JPathName p3;
                            String filename = p2.getName(p2.getNameCount() - 1);
                            LOGGER.info("filename: {}", filename);
                            p3 = res.resolve(filename);
                            LOGGER.info("fichier src: {}", p2);
                            LOGGER.info("fichier cible: {}", p3);
                            try {
                                VFS4JFiles.createDirectories(p3.getParent());
                                if (!VFS4JFiles.exists(p3)) {
                                    VFS4JFiles.copy(p2, p3);
                                    LOGGER.info("copie du fichier '{}' OK", p3);
                                }
                            } catch (IOException e) {
                                throw new UncheckedIOException("Erreur pour copier le fichier " + p3, e);
                            } catch (Exception e) {
                                throw new RuntimeException("Erreur pour copier le fichier " + p3, e);
                            }
                        });
                    }
                } else {
                    LOGGER.info("fichier src: {}", file);
                    LOGGER.info("fichier cible: {}", res);
                    if (!VFS4JFiles.exists(res)) {
                        if (!VFS4JFiles.exists(res.getParent())) {
                            VFS4JFiles.createDirectories(res.getParent());
                        }
                        VFS4JFiles.copy(file, res);
                        LOGGER.info("copie du fichier '{}' OK", res);
                    }
                }
            }
        }
    }

}
