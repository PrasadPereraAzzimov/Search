package com.azzimov.search.controllers;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import com.azzimov.search.common.dto.internals.feedback.ProductFeedback;
import com.azzimov.search.system.spring.AppConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by prasad on 1/4/18.
 * Feedback controller provides the API to persist different types of feedbacks generated from search/product
 * interactions
 */
@RestController
@RequestMapping("/feedback")
public class FeedbackController {
    private AppConfiguration appConfiguration;

    @RequestMapping(value = "/product",
            method = RequestMethod.POST,
            consumes = "application/json",
            produces = "application/json")
    @ResponseBody
    public ResponseEntity<?> persistProductFeedback(@RequestBody ProductFeedback productFeedback) throws Exception{
        ActorSelection selection = appConfiguration.actorSystem().actorSelection("/user/" + AppConfiguration.FEEDBACK_ACTOR);
        // Forward the recieved feedback to the feedback router actor
        selection.tell(productFeedback, ActorRef.noSender());
        return new ResponseEntity<>(HttpStatus.ACCEPTED);
    }

    @Autowired
    public void setAppConfiguration(AppConfiguration appConfiguration) {
        this.appConfiguration = appConfiguration;
    }
}
