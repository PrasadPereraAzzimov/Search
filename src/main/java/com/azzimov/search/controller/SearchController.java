package com.azzimov.search.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by RahulGupta on 2017-11-07.
 */
@RestController
@RequestMapping("/search")
public class SearchController {

    @RequestMapping(value = "/getMessage", method = RequestMethod.GET)
    public String getMessage(){
        Test test = new Test();
        test.setName("Rahul");
        Base base = new AccentChild();
        System.out.print("============== from search contrioller ================");

        return base.printString();
    }
}
