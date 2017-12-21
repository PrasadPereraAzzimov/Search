package com.azzimov.search;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;

/**
 * Created by RahulGupta on 2017-12-21.
 */
@SpringBootApplication
@EnableAutoConfiguration
@ServletComponentScan
public class StartApplication {
    private static final Logger logger = LogManager.getLogger(StartApplication.class);

    public static void main(String[] args){

        SpringApplication.run(StartApplication.class, args);
        logger.info("Application has been started..........");
    }
}
