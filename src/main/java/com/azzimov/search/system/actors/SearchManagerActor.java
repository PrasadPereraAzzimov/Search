package com.azzimov.search.system.actors;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import com.azzimov.search.common.aggregators.AzzimovAggregator;
import com.azzimov.search.common.dto.communications.requests.search.AzzimovSearchRequest;
import com.azzimov.search.common.query.AzzimovBooleanQuery;
import com.azzimov.search.common.query.AzzimovFunctionScoreQuery;
import com.azzimov.search.common.responses.AzzimovSearchResponse;
import com.azzimov.search.listeners.ConfigListener;
import com.azzimov.search.services.feedback.AzzimovFeedbackPersistRequest;
import com.azzimov.search.services.search.executors.SearchExecutorService;
import com.azzimov.search.services.search.aggregators.AzzimovProductSearchAggregatorCreator;
import com.azzimov.search.services.search.filters.AzzimovProductSearchAttributeFilterCreator;
import com.azzimov.search.services.search.params.AzzimovSearchParameters;
import com.azzimov.search.services.search.queries.AzzimovProductSearchExactQueryCreator;
import com.azzimov.search.services.search.queries.AzzimovProductSearchQueryCreator;
import com.azzimov.search.services.search.queries.AzzimovSearchScoreAssimilatorCreator;
import com.azzimov.search.services.search.reponses.AzzimovSearchResponseBuilder;
import com.azzimov.search.services.search.validators.AzzimovSearchRequestValidator;
import com.azzimov.search.system.spring.AppConfiguration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.security.InvalidParameterException;
import java.util.List;


/**
 * Created by prasad on 1/4/18.
 * SearchManagerActor Actor is responsible of handling search requests in Azzimov Search
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class SearchManagerActor extends AbstractActor {
    private static final Logger logger = LogManager.getLogger(SearchManagerActor.class);
    private SearchExecutorService searchExecutorService;
    private ConfigListener configListener;
    private AppConfiguration appConfiguration;

    /**
     * Constructor for FeedbackManagerActor
     * @param searchExecutorService search executor service
     */
    public SearchManagerActor(SearchExecutorService searchExecutorService,
                              ConfigListener configListener,
                              AppConfiguration appConfiguration) {
        this.searchExecutorService = searchExecutorService;
        this.configListener = configListener;
        this.appConfiguration = appConfiguration;
    }

    @Override
    public void preStart() {
        logger.info("Starting the search manager {} {}", getSelf(), getContext().props());
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(AzzimovSearchRequest.class, azzimovSearchRequest -> {
                    AzzimovSearchRequestValidator azzimovSearchRequestValidator =
                            new AzzimovSearchRequestValidator(configListener.getConfigurationHandler());
                    try {
                        AzzimovSearchParameters azzimovSearchParameters = azzimovSearchRequestValidator
                                .validateRequest(azzimovSearchRequest);
                        AzzimovProductSearchQueryCreator azzimovProductSearchQueryCreator =
                                new AzzimovProductSearchQueryCreator(configListener.getConfigurationHandler());
                        AzzimovBooleanQuery azzimovBooleanQuery =
                                azzimovProductSearchQueryCreator.createAzzimovQuery(azzimovSearchParameters, null);
                        com.azzimov.search.common.requests.AzzimovSearchRequest searchRequest =
                                new com.azzimov.search.common.requests.AzzimovSearchRequest();

                        AzzimovProductSearchAttributeFilterCreator azzimovProductSearchFilterCreator =
                                new AzzimovProductSearchAttributeFilterCreator(configListener.getConfigurationHandler());
                        azzimovBooleanQuery = azzimovProductSearchFilterCreator
                                .createAzzimovQuery(azzimovSearchParameters, azzimovBooleanQuery);
                        AzzimovProductSearchExactQueryCreator azzimovProductSearchExactQueryCreator =
                                new AzzimovProductSearchExactQueryCreator(configListener.getConfigurationHandler());
                        AzzimovFunctionScoreQuery azzimovFunctionScoreQuery =
                                azzimovProductSearchExactQueryCreator
                                        .createAzzimovQuery(azzimovSearchParameters, azzimovBooleanQuery);
                        AzzimovSearchScoreAssimilatorCreator azzimovSearchScoreAssimilatorCreator =
                                new AzzimovSearchScoreAssimilatorCreator(configListener.getConfigurationHandler());
                        azzimovFunctionScoreQuery = azzimovSearchScoreAssimilatorCreator
                                .createAzzimovQuery(azzimovSearchParameters, azzimovFunctionScoreQuery);
                        searchRequest.setAzzimovQuery(azzimovFunctionScoreQuery);

                        AzzimovProductSearchAggregatorCreator azzimovProductSearchAggregatorCreator =
                                new AzzimovProductSearchAggregatorCreator();
                        List<AzzimovAggregator> termAggregatorList = azzimovProductSearchAggregatorCreator
                                .createAzzimovQuery(azzimovSearchParameters, null);
                        searchRequest.setAzzimovAggregator(termAggregatorList);

                        AzzimovSearchResponse azzimovSearchResponse = searchExecutorService
                                .getExecutorService().performSearchRequest(searchRequest);
                        AzzimovSearchResponseBuilder azzimovSearchResponseBuilder = new AzzimovSearchResponseBuilder(
                                azzimovSearchResponse,
                                searchRequest);
                        com.azzimov.search.common.dto.communications.responses.search.AzzimovSearchResponse
                                azzimovSearchResponse1 = azzimovSearchResponseBuilder.build();
                        logger.info("Returned search response = {}", azzimovSearchResponse.getTotalHits());
                        getSender().tell(azzimovSearchResponse1, self());
                        logger.info("sending response to = {} {} {}", getContext().sender(), getSender(), sender());
                        persistSearchFeedback(azzimovSearchRequest, azzimovSearchResponse1);
                    } catch (InvalidParameterException invalidParameterException) {
                        logger.error("Invalid parameters are given with the search request");
                    }
                }).build();
    }

    private void persistSearchFeedback(AzzimovSearchRequest azzimovSearchRequest,
                                       com.azzimov.search.common.dto.communications.responses.search.AzzimovSearchResponse azzimovSearchResponse) {
        ActorSelection selection = appConfiguration.actorSystem().actorSelection("/user/" + AppConfiguration.FEEDBACK_ACTOR);
        AzzimovFeedbackPersistRequest azzimovFeedbackPersistRequest = new AzzimovFeedbackPersistRequest();
        azzimovFeedbackPersistRequest.setAzzimovSearchRequest(azzimovSearchRequest);
        azzimovFeedbackPersistRequest.setAzzimovSearchResponse(azzimovSearchResponse);
        logger.error("Sending feedback persist request to  {}", selection);
        selection.tell(azzimovFeedbackPersistRequest, ActorRef.noSender());
    }
}
