package org.simplebackup.simplebackup.service;

import org.simplebackup.simplebackup.model.DirectoryToCompress;
import org.simplebackup.simplebackup.model.MethodCompress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;

@Service
public class BackupService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BackupService.class);

    private BackupDirectoryService backupDirectoryService;

    private BackupZipService backupZipService;

    public BackupService(BackupDirectoryService backupDirectoryService, BackupZipService backupZipService) {
        this.backupDirectoryService = backupDirectoryService;
        this.backupZipService = backupZipService;
    }

    public void backup(DirectoryToCompress directory) throws IOException, GeneralSecurityException {
        LOGGER.info("backup '{}' -> '{}'", directory.pathSource(), directory.pathDestination());

        if (directory.methodCompress() == MethodCompress.Zip) {
            backupZipService.backup(directory);
        } else if (directory.methodCompress() == MethodCompress.NoCompression) {
            backupDirectoryService.backup(directory);
        } else {
            throw new IllegalArgumentException("Method no implemented: " + directory.methodCompress());
        }

        LOGGER.info("backup ok");
    }

}
