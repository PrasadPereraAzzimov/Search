package com.azzimov.search.system.actors;

import akka.actor.AbstractActor;
import com.azzimov.search.common.util.config.ConfigurationHandler;
import com.azzimov.search.services.feedback.AzzimovFeedbackPersistRequest;
import com.azzimov.search.services.feedback.FeedbackPersistManager;
import com.azzimov.search.services.search.executors.SearchExecutorService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import static com.azzimov.search.system.spring.AppConfiguration.FEEDBACK_ACTOR;

/**
 * Created by prasad on 1/4/18.
 * FeedbackManager Actor is responsible of persisting feedbacks in our Azzimov Search
 */
@Component(value = FEEDBACK_ACTOR)
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class FeedbackManagerActor extends AbstractActor {
    private static final Logger logger = LogManager.getLogger(FeedbackManagerActor.class);
    private SearchExecutorService searchExecutorService;

    /**
     * Constructor for FeedbackManagerActor
     * @param searchExecutorService search executor service
     */
    public FeedbackManagerActor(SearchExecutorService searchExecutorService) {
        this.searchExecutorService = searchExecutorService;
    }

    @Override
    public void preStart() {
        logger.info("Starting the feedback manager {} {}", getSelf(), getContext().props());
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(AzzimovFeedbackPersistRequest.class, azzimovFeedbackPersistRequest -> {
                    logger.info("Received a persist feedback request = {} by = {}", azzimovFeedbackPersistRequest,
                            sender());
                    FeedbackPersistManager feedbackPersistManager = new FeedbackPersistManager(searchExecutorService,
                            ConfigurationHandler.getConfigurationHandler());
                    if (azzimovFeedbackPersistRequest.getFeedback() != null) {
                        feedbackPersistManager.persistFeedback(azzimovFeedbackPersistRequest.getFeedback());
                    } else {
                        feedbackPersistManager.persistFeedback(azzimovFeedbackPersistRequest.getAzzimovSearchRequest(),
                                azzimovFeedbackPersistRequest.getAzzimovSearchResponse());
                    }

                }).build();
    }
}
