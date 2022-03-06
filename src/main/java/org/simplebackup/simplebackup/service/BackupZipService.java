package org.simplebackup.simplebackup.service;

import io.github.abarhub.vfs.core.api.VFS4JDefaultFileManager;
import io.github.abarhub.vfs.core.api.VFS4JFiles;
import io.github.abarhub.vfs.core.api.path.VFS4JPathName;
import org.simplebackup.simplebackup.model.DirectoryToCompress;
import org.simplebackup.simplebackup.utils.AESCrypt;
import org.simplebackup.simplebackup.utils.GlobUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class BackupZipService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BackupZipService.class);

    public void backup(DirectoryToCompress directory) throws IOException, GeneralSecurityException {

        var src= directory.pathSource();
        var dest=directory.pathDestination();
        var exclusion=directory.exclude();
        var crypt=directory.crypt();
        var password= directory.password();

        var filename = src.getFilename();
        if (filename == null || src.getFilename().isEmpty()) {
            filename = src.getName();
        }
        String extension=".zip";
        var dest2 = dest.resolve(filename + extension);
        if (VFS4JFiles.exists(dest2)) {
            int i = 2;
            while (VFS4JFiles.exists(dest2)) {
                dest2 = dest.resolve(filename + i + extension);
                i++;
            }
        }
        LOGGER.info("dest={}", dest2);
        if (!VFS4JFiles.exists(dest2.getParent())) {
            VFS4JFiles.createDirectories(dest2.getParent());
        }

        List<PathMatcher> liste = GlobUtils.getPathMatcherList(exclusion);

        try (OutputStream fos = VFS4JFiles.newOutputStream(dest2)) {
            try (ZipOutputStream zipOut = new ZipOutputStream(fos)) {

                zipFile(src, filename, zipOut, liste);
            }
        }

        if(crypt) {

            VFS4JPathName dest3=dest2.getParent().resolve(dest2.getFilename()+".eas");
            LOGGER.info("cryptage de {} vers {}", dest2, dest3);
            AESCrypt aesCrypt=new AESCrypt(password);
            aesCrypt.encrypt(2,dest2,dest3);
            if(VFS4JFiles.exists(dest3)){
                VFS4JFiles.delete(dest2);
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
                    .filter(f -> !GlobUtils.aExclure(f, exclusion))
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


}
