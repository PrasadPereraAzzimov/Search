package com.azzimov.search.system.actors;

import akka.actor.AbstractActor;
import com.azzimov.search.common.dto.communications.responses.search.AzzimovSearchResponse;
import com.azzimov.search.common.dto.externals.Product;
import com.azzimov.search.listeners.ConfigListener;
import com.azzimov.search.services.cache.AzzimovCacheManager;
import com.azzimov.search.services.search.executors.AzzimovAggregateExecutor;
import com.azzimov.search.services.search.executors.SearchExecutorService;
import com.azzimov.search.services.search.executors.product.AzzimovProductAggregateExecutorCreator;
import com.azzimov.search.services.search.learn.LearnStatModelService;
import com.azzimov.search.services.search.params.product.AzzimovSearchParameters;
import com.azzimov.search.system.spring.AppConfiguration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.azzimov.search.system.spring.AppConfiguration.AGGREGATE_ACTOR;

/**
 * Created by prasad on 2/12/18.
 */
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Component(value = AGGREGATE_ACTOR)
public class SearchAggregationManagerActor extends AbstractActor {
    private static final Logger logger = LogManager.getLogger(SearchAggregationManagerActor.class);
    private SearchExecutorService searchExecutorService;
    private ConfigListener configListener;
    private AppConfiguration appConfiguration;
    private Map<String, AzzimovAggregateExecutor> azzimovSearchExecutorMap;
    private LearnStatModelService learnStatModelService;
    private AzzimovCacheManager azzimovCacheManager;

    /**
     * Constructor for FeedbackManagerActor
     *
     * @param searchExecutorService search executor service
     */
    public SearchAggregationManagerActor(SearchExecutorService searchExecutorService,
                                         ConfigListener configListener,
                                         AppConfiguration appConfiguration,
                                         LearnStatModelService learnStatModelService,
                                         AzzimovCacheManager azzimovCacheManager) {
        this.searchExecutorService = searchExecutorService;
        this.configListener = configListener;
        this.appConfiguration = appConfiguration;
        this.azzimovSearchExecutorMap =
                createAzzimovAggregateExecutors(searchExecutorService, configListener, azzimovCacheManager);
        this.learnStatModelService = learnStatModelService;
        this.azzimovCacheManager = azzimovCacheManager;
    }

    @Override
    public void preStart() {
        logger.info("Starting the search aggregation manager {} {}", getSelf(), getContext().props());
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(AzzimovSearchParameters.class, azzimovSearchParameters -> {
                    try {
                        List<AzzimovSearchResponse> azzimovSearchResponseList = new ArrayList<>();
                        for (Map.Entry<String, String> targetTypes :
                                azzimovSearchParameters.getTargetRepositories().entrySet()) {
                            List<AzzimovSearchParameters> azzimovSearchParametersList = new ArrayList<>();
                            azzimovSearchParametersList.add(azzimovSearchParameters);
                            azzimovSearchResponseList =
                                    this.azzimovSearchExecutorMap.get(targetTypes.getKey())
                                            .aggregate(azzimovSearchParametersList);
                            logger.info("Returning aggregate response = {}",
                                    azzimovSearchResponseList.get(0).getAzzimovSearchInfo().getCount());
                        }
                        getSender().tell(azzimovSearchResponseList, self());
                        logger.info("sending response to = {} {} {}", getContext().sender(), getSender(), sender());
                    } catch (InvalidParameterException invalidParameterException) {
                        logger.error("Invalid parameters are given with the search request" + invalidParameterException);
                    }
                }).build();
    }

    private Map<String, AzzimovAggregateExecutor> createAzzimovAggregateExecutors(SearchExecutorService searchExecutorService,
                                                                                  ConfigListener configListener,
                                                                                  AzzimovCacheManager azzimovCacheManager) {
        Map<String, AzzimovAggregateExecutor> azzimovSearchExecutorMap = new HashMap<>();
        AzzimovAggregateExecutor azzimovAggregateExecutor = new AzzimovProductAggregateExecutorCreator(
                configListener.getConfigurationHandler(),
                searchExecutorService,
                azzimovCacheManager);
        azzimovSearchExecutorMap.put(Product.PRODUCT_EXTERNAL_NAME, azzimovAggregateExecutor);
        return azzimovSearchExecutorMap;
    }
}
