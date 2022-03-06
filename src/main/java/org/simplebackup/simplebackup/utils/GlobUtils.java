package org.simplebackup.simplebackup.utils;

import io.github.abarhub.vfs.core.api.VFS4JDefaultFileManager;
import io.github.abarhub.vfs.core.api.path.VFS4JPathName;
import org.simplebackup.simplebackup.service.BackupService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.List;

public class GlobUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobUtils.class);

    public static List<PathMatcher> getPathMatcherList(List<String> exclusion) {
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

    public static boolean aExclure(VFS4JPathName f, List<PathMatcher> liste) {
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

}
