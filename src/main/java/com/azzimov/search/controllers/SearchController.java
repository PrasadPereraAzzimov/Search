package com.azzimov.search.controllers;

import akka.actor.ActorSelection;
import akka.pattern.PatternsCS;
import akka.util.Timeout;
import com.azzimov.search.common.dto.communications.requests.search.AzzimovSearchRequest;
import com.azzimov.search.system.spring.AppConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

/**
 * Created by prasad on 1/9/18.
 * SearchController provides search API for the Azzimov Search application
 */
@RestController
@RequestMapping("/search")
public class SearchController {
    private AppConfiguration appConfiguration;
    private ObjectMapper objectMapper = new ObjectMapper();
    private static final Logger logger = LogManager.getLogger(SearchController.class);
    @RequestMapping(value = "/product",
            method = RequestMethod.POST,
            consumes = "application/json",
            produces = "application/json")
    @ResponseBody
    public ResponseEntity<?> search(@RequestBody AzzimovSearchRequest azzimovSearchRequest) throws Exception {
        ActorSelection selection = appConfiguration.actorSystem().actorSelection("/user/" + AppConfiguration.SEARCH_ACTOR);
        logger.info("Sending search request to = {}", selection);
        final CompletionStage<Object> completionStage  = PatternsCS.ask(selection, azzimovSearchRequest,
                new Timeout(300, TimeUnit.SECONDS));
        Object result = completionStage.toCompletableFuture().get(3000, TimeUnit.SECONDS);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @Autowired
    public void setAppConfiguration(AppConfiguration appConfiguration) {
        this.appConfiguration = appConfiguration;
    }
}
