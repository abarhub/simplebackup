package org.simplebackup.simplebackup.model;

import io.github.abarhub.vfs.core.api.path.VFS4JPathName;

import java.util.List;

public record DirectoryToCompress(VFS4JPathName pathSource,
                                  List<String> exclude,
                                  MethodCompress methodCompress,
                                  VFS4JPathName pathDestination,
                                  boolean crypt,
                                  String password) {
}
