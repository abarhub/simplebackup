package org.simplebackup.simplebackup.service;

import io.github.abarhub.vfs.core.api.VFS4JDefaultFileManager;
import io.github.abarhub.vfs.core.api.VFS4JFiles;
import io.github.abarhub.vfs.core.api.path.VFS4JPathName;
import org.simplebackup.simplebackup.runner.Runner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class BackupService {

    private static final Logger LOGGER = LoggerFactory.getLogger(Runner.class);

    public void backup(VFS4JPathName src, VFS4JPathName dest, boolean zip, List<String> exclusion) throws IOException {
        LOGGER.info("backup '{}' -> '{}'", src, dest);

        if (zip) {
            backupZip(src, dest, exclusion);
        } else {
            backupWithDirectory(src, dest, exclusion);
        }

        LOGGER.info("backup ok");
    }

    private void backupZip(VFS4JPathName src, VFS4JPathName dest, List<String> exclusion) throws IOException {

        var filemanager = VFS4JDefaultFileManager.get();
        Path sourceFile = filemanager.getRealFile(src);
        var filename = src.getFilename();
        if (filename == null || src.getFilename().isEmpty()) {
            filename = src.getName();
        }
        var dest2 = dest.resolve(filename + ".zip");
        if (VFS4JFiles.exists(dest2)) {
            int i = 2;
            while (VFS4JFiles.exists(dest2)) {
                dest2 = dest.resolve(filename + i + ".zip");
                i++;
            }
        }
        LOGGER.info("dest={}", dest2);
        if (!VFS4JFiles.exists(dest2.getParent())) {
            VFS4JFiles.createDirectories(dest2.getParent());
        }

        List<PathMatcher> liste = getPathMatcherList(exclusion);

        try (OutputStream fos = VFS4JFiles.newOutputStream(dest2)) {
            try (ZipOutputStream zipOut = new ZipOutputStream(fos)) {

                zipFile(src, filename, zipOut, liste);
            }
        }
    }

    private void zipFile(VFS4JPathName fileToZip, String fileName, ZipOutputStream zipOut, List<PathMatcher> exclusion) throws IOException {
        if (VFS4JFiles.isHidden(fileToZip)) {
            return;
        }
        if (VFS4JFiles.isDirectory(fileToZip)) {
            if (fileName.endsWith("/")) {
                zipOut.putNextEntry(new ZipEntry(fileName));
                zipOut.closeEntry();
            } else {
                zipOut.putNextEntry(new ZipEntry(fileName + "/"));
                zipOut.closeEntry();
            }

            var children = VFS4JFiles.list(fileToZip)
                    .filter(f -> !aExclure(f, exclusion))
                    .toList();
            for (var childFile : children) {
                LOGGER.info("child: '{}'", childFile);
                zipFile(childFile, fileName + "/" + childFile.getFilename(), zipOut, exclusion);
            }
        } else {
            try (InputStream fis = VFS4JFiles.newInputStream(fileToZip)) {
                LOGGER.info("ajout du fichier '{}'", fileName);
                ZipEntry zipEntry = new ZipEntry(fileName);
                zipOut.putNextEntry(zipEntry);
                byte[] bytes = new byte[1024];
                int length;
                while ((length = fis.read(bytes)) >= 0) {
                    zipOut.write(bytes, 0, length);
                }
            }
        }
    }

    private List<PathMatcher> getPathMatcherList(List<String> exclusion) {
        List<PathMatcher> liste = new ArrayList<>();
        if (exclusion != null) {
            for (String s : exclusion) {
                if (StringUtils.hasText(s)) {
                    PathMatcher matcher =
                            FileSystems.getDefault().getPathMatcher("glob:" + s);
                    liste.add(matcher);
                }
            }
        }
        return liste;
    }

    private boolean aExclure(VFS4JPathName f, List<PathMatcher> liste) {
        if (liste == null || liste.isEmpty()) {
            return false;
        } else {
            var filemanager = VFS4JDefaultFileManager.get();
            Path file = filemanager.getRealFile(f);
            for (var matcher : liste) {
                if (matcher.matches(file)) {
                    LOGGER.debug("match: '{}' = '{}'", matcher, file);
                    return true;
                } else {
                    LOGGER.debug("not match: '{}' = '{}'", matcher, file);
                }
            }
            return false;
        }
    }

    private void backupWithDirectory(VFS4JPathName src, VFS4JPathName dest, List<String> exclusion) throws IOException {

        var exclusionList = getPathMatcherList(exclusion);

        try (var directoryStream = VFS4JFiles.newDirectoryStream(src, (x) -> !aExclure(x, exclusionList))) {
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
