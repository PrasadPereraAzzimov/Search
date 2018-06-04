package com.azzimov.search.system.actors;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.pattern.PatternsCS;
import akka.util.Timeout;
import com.azzimov.search.common.cache.requests.AzzimovCacheRequest;
import com.azzimov.search.common.cache.responses.AzzimovCacheResponse;
import com.azzimov.search.common.cache.service.CacheService;
import com.azzimov.search.common.dto.communications.requests.search.AzzimovSearchRequest;
import com.azzimov.search.common.dto.communications.responses.search.AzzimovSearchResponse;
import com.azzimov.search.common.dto.externals.Product;
import com.azzimov.search.common.util.config.ConfigurationHandler;
import com.azzimov.search.common.util.config.SearchConfiguration;
import com.azzimov.search.services.cache.AzzimovCacheManager;
import com.azzimov.search.services.feedback.AzzimovFeedbackPersistRequest;
import com.azzimov.search.services.search.executors.AzzimovSearchExecutor;
import com.azzimov.search.services.search.executors.SearchExecutorService;
import com.azzimov.search.services.search.executors.product.AzzimovProductSearchExecutor;
import com.azzimov.search.services.search.learn.LearnCentroidCluster;
import com.azzimov.search.services.search.learn.LearnStatModelService;
import com.azzimov.search.services.search.learn.SessionCentroidModelCluster;
import com.azzimov.search.services.search.params.product.AzzimovSearchParameters;
import com.azzimov.search.services.search.validators.product.AzzimovSearchRequestValidator;
import com.azzimov.search.system.spring.AppConfiguration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

import static com.azzimov.search.system.actors.SessionLearningGeneratorActor.DEFAULT_MODEL_KEY;
import static com.azzimov.search.system.spring.AppConfiguration.SEARCH_ACTOR;


/**
 * Created by prasad on 1/4/18.
 * SearchManagerActor Actor is responsible of handling search requests in Azzimov Search
 */
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Component(value = SEARCH_ACTOR)
public class SearchManagerActor extends AbstractActor {
    private static final Logger logger = LogManager.getLogger(SearchManagerActor.class);
    private SearchExecutorService searchExecutorService;
    private AppConfiguration appConfiguration;
    private Map<String, AzzimovSearchExecutor> azzimovSearchExecutorMap;
    private LearnStatModelService learnStatModelService;
    private AzzimovCacheManager azzimovCacheManager;
    private ConfigurationHandler configurationHandler = ConfigurationHandler.getInstance();
    /**
     * Constructor for FeedbackManagerActor
     * @param searchExecutorService search executor service
     */
    public SearchManagerActor(SearchExecutorService searchExecutorService,
                              AppConfiguration appConfiguration,
                              LearnStatModelService learnStatModelService,
                              AzzimovCacheManager azzimovCacheManager) {
        this.searchExecutorService = searchExecutorService;
        this.appConfiguration = appConfiguration;
        this.azzimovSearchExecutorMap =
                createAzzimovSearchExecutors(searchExecutorService, learnStatModelService);
        this.learnStatModelService = learnStatModelService;
        this.azzimovCacheManager = azzimovCacheManager;
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
                            new AzzimovSearchRequestValidator(configurationHandler);
                    try {
                        AzzimovSearchParameters azzimovSearchParameters = azzimovSearchRequestValidator
                                .validateRequest(azzimovSearchRequest);
                        CompletionStage<Object> aggregateResponse = retrieveGuidanceResponse(azzimovSearchParameters);
                        CompletionStage<Object> suggestResponse = retrieveSuggestionResponse(azzimovSearchParameters);
                        List<AzzimovSearchResponse> azzimovSearchResponseList = new ArrayList<>();
                        for (Map.Entry<String, String> targetTypes :
                                azzimovSearchParameters.getTargetRepositories().entrySet()) {
                            List<AzzimovSearchParameters> azzimovSearchParametersList = new ArrayList<>();
                            azzimovSearchParametersList.add(azzimovSearchParameters);

                            List<LearnCentroidCluster> learnCentroidClusters = new ArrayList<>();
                            if (learnStatModelService.getGuidanceLearnCentroidCluster() != null)
                                learnCentroidClusters.add(learnStatModelService.getGuidanceLearnCentroidCluster());
                            LearnCentroidCluster learnCentroidCluster = retrieveMemberModel(azzimovSearchRequest);
                            if (learnCentroidCluster != null)
                                learnCentroidClusters.add(learnCentroidCluster);
                            LearnCentroidCluster sessionLearnCentroidCluster = retrieveSessionModel(azzimovSearchRequest);
                            if (sessionLearnCentroidCluster != null)
                                learnCentroidClusters.add(sessionLearnCentroidCluster);
                            azzimovSearchParameters.setLearnCentroidClusterList(learnCentroidClusters);
                            azzimovSearchResponseList =
                                    this.azzimovSearchExecutorMap.get(targetTypes.getKey())
                                            .search(azzimovSearchParametersList);
                            // Retrieve aggregation response and combine with final searcg result response
                            List<AzzimovSearchResponse> azzimovAggregateResponseList = (List<AzzimovSearchResponse>)
                                    aggregateResponse.toCompletableFuture().get(3000, TimeUnit.SECONDS);
                            List<AzzimovSearchResponse> azzimovSuggestResponseList = (List<AzzimovSearchResponse>)
                                    suggestResponse.toCompletableFuture().get(3000, TimeUnit.SECONDS);
                            int index = 0;
                            for (AzzimovSearchResponse azzimovSearchResponse : azzimovSearchResponseList) {
                                azzimovSearchResponse
                                        .getAzzimovSearchResponseParameter().setGuidance(
                                                azzimovAggregateResponseList.get(index++)
                                                        .getAzzimovSearchResponseParameter().getGuidance());
                                azzimovSearchResponse.setAzzimovSuggestionResponse(
                                        azzimovSuggestResponseList.get(0).getAzzimovSuggestionResponse());
                            }
                            logger.info("Returning search response = {}",
                                    azzimovSearchResponseList.get(0).getAzzimovSearchInfo().getCount());
                        }
                        getSender().tell(azzimovSearchResponseList.get(0), self());
                        logger.info("sending response to = {} {} {}", getContext().sender(), getSender(), sender());
                        persistSearchFeedback(azzimovSearchRequest, azzimovSearchResponseList.get(0));
                    } catch (InvalidParameterException invalidParameterException) {
                        logger.error("Invalid parameters are given with the search request" + invalidParameterException);
                    }
                }).build();
    }

    private void persistSearchFeedback(AzzimovSearchRequest azzimovSearchRequest,
                                       AzzimovSearchResponse azzimovSearchResponse) {
        ActorSelection selection = appConfiguration.actorSystem().actorSelection("/user/" + AppConfiguration.FEEDBACK_ACTOR);
        AzzimovFeedbackPersistRequest azzimovFeedbackPersistRequest = new AzzimovFeedbackPersistRequest();
        azzimovFeedbackPersistRequest.setAzzimovSearchRequest(azzimovSearchRequest);
        azzimovFeedbackPersistRequest.setAzzimovSearchResponse(azzimovSearchResponse);
        logger.info("Sending feedback persist request to  {}", selection);
        selection.tell(azzimovFeedbackPersistRequest, ActorRef.noSender());
    }


    private CompletionStage<Object> retrieveGuidanceResponse(AzzimovSearchParameters azzimovSearchParameters) {
        ActorSelection selection = appConfiguration.actorSystem().actorSelection("/user/" + AppConfiguration.AGGREGATE_ACTOR);
        logger.info("Sending aggregate request to  {}", selection);
        final CompletionStage<Object> completionStage  = PatternsCS.ask(selection, azzimovSearchParameters,
                new Timeout(300, TimeUnit.SECONDS));
        return completionStage;
    }

    private CompletionStage<Object> retrieveSuggestionResponse(AzzimovSearchParameters azzimovSearchParameters) {
        ActorSelection selection = appConfiguration.actorSystem().actorSelection("/user/" +
                AppConfiguration.SUGGEST_AUTOCOMPLETE_ACTOR);
        logger.info("Sending suggest request to  {}", selection);
        final CompletionStage<Object> completionStage  = PatternsCS.ask(selection, azzimovSearchParameters,
                new Timeout(300, TimeUnit.SECONDS));
        return completionStage;
    }

    private Map<String, AzzimovSearchExecutor> createAzzimovSearchExecutors(SearchExecutorService searchExecutorService,
                                                                            LearnStatModelService learnStatModelService) {
        Map<String, AzzimovSearchExecutor> azzimovSearchExecutorMap = new HashMap<>();
        AzzimovSearchExecutor azzimovSearchExecutor = new AzzimovProductSearchExecutor(
                configurationHandler,
                searchExecutorService);
        azzimovSearchExecutorMap.put(Product.PRODUCT_EXTERNAL_NAME, azzimovSearchExecutor);
        return azzimovSearchExecutorMap;
    }

    private LearnCentroidCluster retrieveMemberModel(AzzimovSearchRequest azzimovSearchRequest) {
        String modelKey = LearnCentroidCluster.CENTROID_GUIDANCE_KEY + "-" +
                azzimovSearchRequest.getAzzimovUserRequestParameters().getMemberId();
        try {
            return LearnStatModelService.retrieveGuidanceLearningModelManager(configurationHandler, azzimovCacheManager, modelKey);
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private LearnCentroidCluster retrieveSessionModel(AzzimovSearchRequest azzimovSearchRequest) {
        String cacheKey = configurationHandler
                .getStringConfig(SearchConfiguration.SESSION_LEVEL_CENTROID_MODEL, DEFAULT_MODEL_KEY) +
                azzimovSearchRequest.getAzzimovUserRequestParameters().getSessionId();
        String bucket = azzimovCacheManager.getCouchbaseConfiguration().getBuckets().get(0);
        AzzimovCacheRequest<SessionCentroidModelCluster> azzimovCacheRequest =
                new AzzimovCacheRequest<>(bucket, cacheKey);
        CacheService<SessionCentroidModelCluster> learningModelCacheService  =
                azzimovCacheManager.getCacheProvider(SessionCentroidModelCluster.class);
        AzzimovCacheResponse<SessionCentroidModelCluster> azzimovCacheResponse;
        SessionCentroidModelCluster sessionCentroidModelClusterPrevious = null;
        try {
            azzimovCacheResponse = learningModelCacheService.get(azzimovCacheRequest);
            if (azzimovCacheResponse.getObjectType() != null) {
                sessionCentroidModelClusterPrevious = azzimovCacheResponse.getObjectType();
            } else {
                sessionCentroidModelClusterPrevious = new SessionCentroidModelCluster();
                Map<String, List<SessionCentroidModelCluster.SessionCentroid>> sessionLearningModelClusterMap = new HashMap<>();
                sessionCentroidModelClusterPrevious.setSessionLearningModelClusterMap(sessionLearningModelClusterMap);
                sessionCentroidModelClusterPrevious.setCreationTime(DateTime.now(DateTimeZone.UTC));
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        return sessionCentroidModelClusterPrevious;
    }
}
