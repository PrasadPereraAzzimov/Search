package com.azzimov.search.system.actors;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import com.azzimov.search.common.dto.communications.requests.search.AzzimovSearchRequest;
import com.azzimov.search.common.dto.communications.responses.search.AzzimovSearchResponse;
import com.azzimov.search.common.dto.externals.Product;
import com.azzimov.search.common.dto.externals.Retailer;
import com.azzimov.search.listeners.ConfigListener;
import com.azzimov.search.services.cache.AzzimovCacheManager;
import com.azzimov.search.services.feedback.AzzimovFeedbackPersistRequest;
import com.azzimov.search.services.search.executors.AzzimovSearchExecutor;
import com.azzimov.search.services.search.executors.SearchExecutorService;
import com.azzimov.search.services.search.executors.product.AzzimovProductSearchExecutor;
import com.azzimov.search.services.search.executors.retailer.AzzimovRetailerSearchExecutor;
import com.azzimov.search.services.search.learn.LearnCentroidCluster;
import com.azzimov.search.services.search.learn.LearnStatModelService;
import com.azzimov.search.services.search.params.product.AzzimovSearchParameters;
import com.azzimov.search.services.search.validators.product.AzzimovSearchRequestValidator;
import com.azzimov.search.system.spring.AppConfiguration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
    private ConfigListener configListener;
    private AppConfiguration appConfiguration;
    private Map<String, AzzimovSearchExecutor> azzimovSearchExecutorMap;
    private LearnStatModelService learnStatModelService;
    private AzzimovCacheManager azzimovCacheManager;

    /**
     * Constructor for FeedbackManagerActor
     * @param searchExecutorService search executor service
     */
    public SearchManagerActor(SearchExecutorService searchExecutorService,
                              ConfigListener configListener,
                              AppConfiguration appConfiguration,
                              LearnStatModelService learnStatModelService,
                              AzzimovCacheManager azzimovCacheManager) {
        this.searchExecutorService = searchExecutorService;
        this.configListener = configListener;
        this.appConfiguration = appConfiguration;
        this.azzimovSearchExecutorMap =
                createAzzimovSearchExecutors(searchExecutorService, configListener, learnStatModelService);
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
                            new AzzimovSearchRequestValidator(configListener.getConfigurationHandler());
                    try {
                        AzzimovSearchParameters azzimovSearchParameters = azzimovSearchRequestValidator
                                .validateRequest(azzimovSearchRequest);
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
                            azzimovSearchParameters.setLearnCentroidClusterList(learnCentroidClusters);
                            azzimovSearchResponseList =
                                    this.azzimovSearchExecutorMap.get(targetTypes.getKey())
                                            .search(azzimovSearchParametersList);
                            logger.info("Returning search response = {}",
                                    azzimovSearchResponseList.get(0).getAzzimovSearchInfo().getCount());
                        }
                        getSender().tell(azzimovSearchResponseList, self());
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

    private Map<String, AzzimovSearchExecutor> createAzzimovSearchExecutors(SearchExecutorService searchExecutorService,
                                                                            ConfigListener configListener,
                                                                            LearnStatModelService learnStatModelService) {
        Map<String, AzzimovSearchExecutor> azzimovSearchExecutorMap = new HashMap<>();
        AzzimovSearchExecutor azzimovSearchExecutor = new AzzimovProductSearchExecutor(
                configListener.getConfigurationHandler(),
                searchExecutorService);
        azzimovSearchExecutorMap.put(Product.PRODUCT_EXTERNAL_NAME, azzimovSearchExecutor);

        azzimovSearchExecutor = new AzzimovRetailerSearchExecutor(configListener.getConfigurationHandler(),
                searchExecutorService);
        azzimovSearchExecutorMap.put(Retailer.RETAILER_EXTERNAL_NAME, azzimovSearchExecutor);
        return azzimovSearchExecutorMap;
    }

    private LearnCentroidCluster retrieveMemberModel(AzzimovSearchRequest azzimovSearchRequest) {
        String modelKey = LearnCentroidCluster.CENTROID_GUIDANCE_KEY + "-" +
                azzimovSearchRequest.getAzzimovUserRequestParameters().getMemberId();
        try {
            return LearnStatModelService.retrieveGuidanceLearningModelManager(configListener, azzimovCacheManager, modelKey);
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
}
