package com.azzimov.search;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.context.annotation.ComponentScan;

/**
 * Created by RahulGupta on 2017-11-08.
 */

@SpringBootApplication
@EnableAutoConfiguration
@ComponentScan
@ServletComponentScan
public class StartApplication {
    public static void main(String[] args){
        SpringApplication.run(StartApplication.class, args);
    }
}
