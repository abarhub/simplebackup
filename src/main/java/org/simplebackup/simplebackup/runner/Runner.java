package org.simplebackup.simplebackup.runner;

import io.github.abarhub.vfs.core.api.VFS4JFiles;
import io.github.abarhub.vfs.core.api.path.VFS4JPathName;
import io.github.abarhub.vfs.core.api.path.VFS4JPaths;
import org.simplebackup.simplebackup.model.DirectoryToCompress;
import org.simplebackup.simplebackup.model.MethodCompress;
import org.simplebackup.simplebackup.properties.ListDirectories;
import org.simplebackup.simplebackup.service.BackupService;
import org.simplebackup.simplebackup.utils.AESCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Service
public class Runner implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(Runner.class);

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    @Autowired
    private ListDirectories directorySource;

    @Value("${directory-destination}")
    private String directoryDestination;

    @Value("${password:}")
    private String password;

    @Value("${decrypt-sources:}")
    private String decryptSource;

    @Value("${compress:}")
    private String compressConfig;

    @Value("${crypt:}")
    private Boolean cryptConfig;

    @Autowired
    private BackupService backupService;

    @Override
    public void run(ApplicationArguments args) throws Exception {

        if (args.getOptionNames().contains("decrypt")) {
            LOGGER.info("décryptage");
            if (!StringUtils.hasText(decryptSource)) {
                throw new IOException("source path '" + decryptSource + "' dont exists");
            }
            try (var stream = VFS4JFiles.list(getPath(decryptSource))) {
                var list = stream.toList();
                for (var file : list) {
                    if (VFS4JFiles.isRegularFile(file) && file.endsWith(".eas")) {
                        VFS4JPathName dest = file.getParent().resolve(file.getFilename().substring(0, file.getFilename().length() - 4));
                        int i = 2;
                        while (VFS4JFiles.exists(dest)) {
                            dest = file.getParent().resolve(file.getFilename().substring(0, file.getFilename().length() - 4) + "." + i);
                            i++;
                        }
                        LOGGER.info("décryptage {} -> {}", file, dest);
                        AESCrypt aesCrypt = new AESCrypt(password);
                        aesCrypt.decrypt(file, dest);
                    }
                }
            }
        } else {
            LOGGER.info("backup");
            if (directorySource != null && directorySource.getList() != null && StringUtils.hasText(directoryDestination)) {
                var pathDest = getPath(directoryDestination);
                if (!VFS4JFiles.exists(pathDest)) {
                    throw new IOException("Destination path '" + directoryDestination + "' dont exists");
                }
                pathDest = pathDest.resolve("backup_" + FORMATTER.format(LocalDateTime.now()));
                if (!VFS4JFiles.exists(pathDest)) {
                    VFS4JFiles.createDirectories(pathDest);
                }
                Map<String, Duration> map = new HashMap<>();
                MethodCompress methodCompress = MethodCompress.NoCompression;
                if (StringUtils.hasText(compressConfig)) {
                    if (Objects.equals(compressConfig, "zip")) {
                        LOGGER.info("zip");
                        methodCompress = MethodCompress.Zip;
                    } else {
                        throw new IllegalArgumentException("Invalid compression '" + compressConfig + "'");
                    }
                }

                boolean crypt = cryptConfig!=null&&cryptConfig;
                if (crypt) {
                    LOGGER.info("cryptage activé");
                    if(!StringUtils.hasText(password)){
                        throw new IllegalArgumentException("Crypt without password");
                    }
                }

                for (var dir : directorySource.getList()) {
                    for (var src : dir.getPathList()) {
                        var pathSrc = getPath(src);
                        if (!VFS4JFiles.exists(pathSrc)) {
                            throw new IOException("Source path '" + src + "' dont exists");
                        }

                        DirectoryToCompress directory = new DirectoryToCompress(pathSrc,
                                dir.getExcludeList(), methodCompress, pathDest,
                                crypt, (crypt) ? password : null);

                        var debut = Instant.now();
                        backupService.backup(directory);
                        var fin = Instant.now();
                        map.put(dir.getName() + "/" + src, Duration.between(debut, fin));
                    }
                }
                LOGGER.info("duree: {}", map);
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
