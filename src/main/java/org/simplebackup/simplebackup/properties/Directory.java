package org.simplebackup.simplebackup.properties;

import com.google.common.base.Splitter;

import java.util.List;

public class Directory {

    private String name;
    private String path;
    private List<String> pathList;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
        if (path == null) {
            pathList = null;
        } else {
            pathList = Splitter.on(";").splitToList(path);
        }
    }

    public List<String> getPathList() {
        return pathList;
    }

    public void setPathList(List<String> pathList) {
        this.pathList = pathList;
    }
}