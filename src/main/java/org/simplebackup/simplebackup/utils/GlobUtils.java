package org.simplebackup.simplebackup.utils;

import io.github.abarhub.vfs.core.api.VFS4JDefaultFileManager;
import io.github.abarhub.vfs.core.api.VFS4JFileManager;
import io.github.abarhub.vfs.core.api.path.VFS4JPathMatcher;
import io.github.abarhub.vfs.core.api.path.VFS4JPathName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class GlobUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobUtils.class);

    public static List<VFS4JPathMatcher> getPathMatcherList(List<String> exclusion, VFS4JFileManager fileManager) {
        List<VFS4JPathMatcher> liste = new ArrayList<>();
        if (exclusion != null) {
            for (String s : exclusion) {
                if (StringUtils.hasText(s)) {
                    VFS4JPathMatcher matcher = fileManager.matcher("glob:" + s);
                    liste.add(matcher);
                }
            }
        }
        return liste;
    }

    public static boolean aExclure(VFS4JPathName f, List<VFS4JPathMatcher> liste) {
        if (liste == null || liste.isEmpty()) {
            return false;
        } else {
            var filemanager = VFS4JDefaultFileManager.get();
            Path file = filemanager.getRealFile(f);
            for (var matcher : liste) {
                if (matcher.matches(f)) {
                    LOGGER.debug("match: '{}' = '{}'", matcher, f);
                    return true;
                } else {
                    LOGGER.debug("not match: '{}' = '{}'", matcher, f);
                }
            }
            return false;
        }
    }

}
