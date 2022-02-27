package org.simplebackup.simplebackup.runner;

import io.github.abarhub.vfs.core.api.VFS4JFiles;
import io.github.abarhub.vfs.core.api.path.VFS4JPathName;
import io.github.abarhub.vfs.core.api.path.VFS4JPaths;
import org.simplebackup.simplebackup.properties.ListDirectories;
import org.simplebackup.simplebackup.service.BackupService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class Runner implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(Runner.class);

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    @Autowired
    private ListDirectories directorySource;

    @Value("${directory-destination}")
    private String directoryDestination;

    @Autowired
    private BackupService backupService;

    @Override
    public void run(ApplicationArguments args) throws Exception {

        if (directorySource != null && directorySource.getList() != null && StringUtils.hasText(directoryDestination)) {
            var pathDest = getPath(directoryDestination);
            if (!VFS4JFiles.exists(pathDest)) {
                throw new IOException("Destination path '" + directoryDestination + "' dont exists");
            }
            pathDest = pathDest.resolve("backup_"+FORMATTER.format(LocalDateTime.now()));
            if (!VFS4JFiles.exists(pathDest)) {
                VFS4JFiles.createDirectories(pathDest);
            }
            var zip=false;
            zip=true;

            for (var dir : directorySource.getList()) {
                for (var src : dir.getPathList()) {
                    var pathSrc = getPath(src);
                    if (!VFS4JFiles.exists(pathSrc)) {
                        throw new IOException("Source path '" + src + "' dont exists");
                    }

                    backupService.backup(pathSrc, pathDest, zip, dir.getExcludeList());
                }
            }
        }
    }

    private VFS4JPathName getPath(String path) {
        if (path == null) {
            throw new IllegalArgumentException("Path is empty");
        }
        var i = path.indexOf(':');
        if (i < 0) {
            throw new IllegalArgumentException("Path must contains ':' (path='" + path + "')");
        }
        var name = path.substring(0, i);
        var path2 = path.substring(i + 1);
        return VFS4JPaths.get(name, path2);
    }
}
