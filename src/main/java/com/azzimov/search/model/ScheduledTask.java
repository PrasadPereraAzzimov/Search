package com.azzimov.search.model;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Created by RahulGupta on 2017-11-15.
 */
@Component
public class ScheduledTask {

    @Scheduled(cron = "0/1 * * ? * ?")
    public void run(){
        System.out.println("Task completed");
    }
}
