package com.azzimov.search.system.actors;

import akka.actor.AbstractActor;
import com.azzimov.search.common.dto.communications.responses.search.AzzimovSearchResponse;
import com.azzimov.search.listeners.ConfigListener;
import com.azzimov.search.services.cache.AzzimovCacheManager;
import com.azzimov.search.services.search.executors.SearchExecutorService;
import com.azzimov.search.services.search.executors.product.AzzimovProductSuggestionExecutor;
import com.azzimov.search.services.search.params.product.AzzimovSearchParameters;
import com.azzimov.search.system.spring.AppConfiguration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import static com.azzimov.search.system.spring.AppConfiguration.SUGGEST_AUTOCOMPLETE_ACTOR;

/**
 * Created by prasad on 4/24/18.
 *
 */
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Component(value = SUGGEST_AUTOCOMPLETE_ACTOR)
public class SuggestAutocompleteManagerActor extends AbstractActor {
    private static final Logger logger = LogManager.getLogger(SearchManagerActor.class);
    private ConfigListener configListener;
    private AppConfiguration appConfiguration;
    private AzzimovCacheManager azzimovCacheManager;
    private AzzimovProductSuggestionExecutor azzimovProductSuggestionExecutor;

    /**
     * Constructor for FeedbackManagerActor
     * @param searchExecutorService search executor service
     */
    public SuggestAutocompleteManagerActor(SearchExecutorService searchExecutorService,
                                           ConfigListener configListener,
                                           AppConfiguration appConfiguration,
                                           AzzimovCacheManager azzimovCacheManager) {
        this.configListener = configListener;
        this.appConfiguration = appConfiguration;
        this.azzimovCacheManager = azzimovCacheManager;
        this.azzimovProductSuggestionExecutor = new AzzimovProductSuggestionExecutor(configListener.getConfigurationHandler(),
                searchExecutorService);
    }


    @Override
    public void preStart() {
        logger.info("Starting the suggest/autocomplete manager {} {}", getSelf(), getContext().props());
    }

    @Override
    public AbstractActor.Receive createReceive() {
        return receiveBuilder()
                .match(AzzimovSearchParameters.class, azzimovSearchParameters -> {
                    try {
                        List<AzzimovSearchResponse> azzimovSearchResponseList = new ArrayList<>();
                        for (Map.Entry<String, String> targetTypes :
                                azzimovSearchParameters.getTargetRepositories().entrySet()) {
                            List<AzzimovSearchParameters> azzimovSearchParametersList = new ArrayList<>();
                            azzimovSearchParametersList.add(azzimovSearchParameters);
                            List<AzzimovSearchResponse> azzimovSuggestionResponses =
                                    this.azzimovProductSuggestionExecutor.search(azzimovSearchParametersList);
                            azzimovSearchResponseList.addAll(azzimovSuggestionResponses);
                            // Retrieve aggregation response and combine with final searcg result response
                            logger.info("Returning search response = {}",
                                    azzimovSuggestionResponses.get(0)
                                            .getAzzimovSuggestionResponse().getSuggestions().size());
                        }
                        getSender().tell(azzimovSearchResponseList, self());
                        logger.info("sending response to = {} {} {}", getContext().sender(), getSender(), sender());
                    } catch (InvalidParameterException invalidParameterException) {
                        logger.error("Invalid parameters are given with the search request" + invalidParameterException);
                    }
                }).build();
    }
}
