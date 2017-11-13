package com.azzimov.search.model;

import org.springframework.stereotype.Component;

/**
 * Created by RahulGupta on 2017-11-09.
 */
@Component
public class GreetingService {

    public GreetingService(){}

    public String greet(String name){
        return "Hello, " + name;
    }
}
