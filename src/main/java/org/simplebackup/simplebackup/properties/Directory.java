package org.simplebackup.simplebackup.properties;

import com.google.common.base.Splitter;

import java.util.List;

public class Directory {

    private String name;
    private String path;
    private List<String> pathList;
    private String exclude;
    private List<String> excludeList;

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

    public String getExclude() {
        return exclude;
    }

    public void setExclude(String exclude) {
        this.exclude = exclude;
        if (exclude == null) {
            excludeList = null;
        } else {
            excludeList = Splitter.on(";").splitToList(exclude);
        }
    }

    public List<String> getExcludeList() {
        return excludeList;
    }

    public void setExcludeList(List<String> excludeList) {
        this.excludeList = excludeList;
    }
}
