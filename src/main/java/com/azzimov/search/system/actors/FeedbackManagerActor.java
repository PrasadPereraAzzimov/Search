package com.azzimov.search.system.actors;

import akka.actor.AbstractActor;
import com.azzimov.search.common.dto.internals.feedback.ProductFeedback;
import com.azzimov.search.common.query.AzzimovMatchAllQuery;
import com.azzimov.search.common.requests.AzzimovIndexPersistRequest;
import com.azzimov.search.common.responses.AzzimovIndexResponse;
import com.azzimov.search.common.responses.AzzimovSourceResponse;
import com.azzimov.search.services.SearchExecutorService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by prasad on 1/4/18.
 * FeedbackManager Actor is responsible of persisting feedbacks in our Azzimov Search
 */
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
                .match(ProductFeedback.class, productFeedback -> {
                    logger.info("Received a feedback = {} by = {}", productFeedback, getSelf());
                    AzzimovIndexPersistRequest<ProductFeedback> azzimovIndexPersistRequest =
                            new AzzimovIndexPersistRequest<>();
                    Map<String, ProductFeedback> productFeedbackMap = new HashMap<>();
                    productFeedbackMap.put("" + productFeedback.getOriginalProductId(), productFeedback);
                    azzimovIndexPersistRequest.setSource(productFeedbackMap);
                    List<String> documentTypes = new ArrayList<>();
                    documentTypes.add("product");
                    azzimovIndexPersistRequest.setAzzimovQuery(new AzzimovMatchAllQuery("feedback", documentTypes));
                    AzzimovSourceResponse<AzzimovIndexResponse> azzimovSourceResponse =
                    searchExecutorService.getExecutorService().performPersistRequest(azzimovIndexPersistRequest);
                    logger.info("Persisted document = {}", azzimovSourceResponse.getObjectType().get(0).getStatus());
                }).build();
    }
}
