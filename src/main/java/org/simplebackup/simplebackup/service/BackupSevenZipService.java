package org.simplebackup.simplebackup.service;

import io.github.abarhub.vfs.core.api.VFS4JFiles;
import org.simplebackup.simplebackup.model.DirectoryToCompress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class BackupSevenZipService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BackupSevenZipService.class);

    private SevenZipService sevenZipService;

    public BackupSevenZipService(SevenZipService sevenZipService) {
        this.sevenZipService = sevenZipService;
    }

    public void backup(DirectoryToCompress directory) throws IOException, InterruptedException {

        var src= directory.pathSource();
        var dest=directory.pathDestination();
        var exclusion=directory.exclude();
        var crypt=directory.crypt();
        var password= directory.password();

        var filename = src.getFilename();
        if (filename == null || src.getFilename().isEmpty()) {
            filename = src.getName();
        }
        String extension=".7z";
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
        sevenZipService.compress(src, dest2, exclusion,crypt,password);
    }

}
