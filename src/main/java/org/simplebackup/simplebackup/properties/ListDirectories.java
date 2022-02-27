package org.simplebackup.simplebackup.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationProperties(prefix = "directory-sources")
public class ListDirectories {

    private List<Directory> list;

    public List<Directory> getList() {
        return list;
    }

    public void setList(List<Directory> list) {
        this.list = list;
    }
}
