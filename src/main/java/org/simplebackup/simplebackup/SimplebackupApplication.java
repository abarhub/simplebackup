package org.simplebackup.simplebackup;

import org.simplebackup.simplebackup.runner.Runner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SimplebackupApplication {

    private static final Logger LOGGER = LoggerFactory.getLogger(SimplebackupApplication.class);

    public static void main(String[] args) {
        LOGGER.info("debut");
        SpringApplication.run(SimplebackupApplication.class, args);
        LOGGER.info("fin");
    }

}
