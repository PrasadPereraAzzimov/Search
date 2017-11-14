package com.azzimov.search.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by RahulGupta on 2017-11-07.
 */
@RestController
@RequestMapping("/search")
public class SearchController {

    @Autowired
    private SearchHandler searchHandler;

    @RequestMapping(value = "/getMessage", method = RequestMethod.GET)
    public String getMessage(){
        Test test = new Test();
        test.setName("Rahul");
        Base base = new AccentChild();
        System.out.println("============== from search contrioller ================");

        System.out.println("==========================================================");
        System.out.println(searchHandler.getMessage());
        System.out.println("==========================================================");

        return base.printString();
    }
}
