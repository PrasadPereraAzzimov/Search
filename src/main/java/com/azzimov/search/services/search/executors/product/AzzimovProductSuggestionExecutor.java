package com.azzimov.search.services.search.executors.product;

import com.azzimov.search.common.dto.communications.responses.search.AzzimovSearchResponse;
import com.azzimov.search.common.dto.communications.responses.search.AzzimovSuggestionResponse;
import com.azzimov.search.common.dto.internals.feedback.FeedbackType;
import com.azzimov.search.common.query.AzzimovMatchAllQuery;
import com.azzimov.search.common.requests.AzzimovMultiSearchRequest;
import com.azzimov.search.common.suggestors.AzzimovSuggestor;
import com.azzimov.search.common.util.config.ConfigurationHandler;
import com.azzimov.search.common.util.config.SearchConfiguration;
import com.azzimov.search.listeners.ConfigListener;
import com.azzimov.search.services.search.executors.AzzimovSearchExecutor;
import com.azzimov.search.services.search.executors.SearchExecutorService;
import com.azzimov.search.services.search.params.product.AzzimovSearchParameters;
import com.azzimov.search.services.search.reponses.AzzimovSearchResponseBuilder;
import com.azzimov.search.services.search.suggestors.product.AzzimovProductSuggestionCreator;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Created by prasad on 4/19/18.
 * AzzimovProductSuggestionExecutor executes suggestion based queries and return search/suggestion results
 */
public class AzzimovProductSuggestionExecutor  extends AzzimovSearchExecutor {
    private ConfigurationHandler configurationHandler;
    private SearchExecutorService searchExecutorService;

    public AzzimovProductSuggestionExecutor(ConfigurationHandler configurationHandler,
                                            SearchExecutorService searchExecutorService) {
        this.configurationHandler = configurationHandler;
        this.searchExecutorService = searchExecutorService;
    }

    @Override
    public List<AzzimovSearchResponse> search(List<AzzimovSearchParameters> azzimovSearchParameters)
            throws IllegalAccessException, IOException, InstantiationException {
        AzzimovMultiSearchRequest azzimovMultiSearchRequest = new AzzimovMultiSearchRequest();
        AzzimovProductSuggestionCreator azzimovProductSuggestionCreator = new AzzimovProductSuggestionCreator();
        List<String> documentTypes = new ArrayList<>();
        documentTypes.add(FeedbackType.QUERY.toString());
        List<Object> targetRepositoriesConfigs = configurationHandler
                .getObjectConfigList(SearchConfiguration.SEARCH_DOC_TARGET_INDEXES);
        Map<String, String> targetRepositories = ConfigListener.retrieveTargetRepositoriesforDocuments(
                targetRepositoriesConfigs,
                configurationHandler);
        String targetRepository = targetRepositories.values().iterator().next();

        List<AzzimovSuggestor> azzimovSuggestorList =
                azzimovProductSuggestionCreator.createAzzimovSuggestion(azzimovSearchParameters.get(0));
        List<com.azzimov.search.common.requests.AzzimovSearchRequest> azzimovSearchRequestList = new ArrayList<>();
        for (AzzimovSuggestor azzimovSuggestor : azzimovSuggestorList) {
            com.azzimov.search.common.requests.AzzimovSearchRequest searchRequest =
                    new com.azzimov.search.common.requests.AzzimovSearchRequest();
            AzzimovMatchAllQuery azzimovMatchAllQuery = new AzzimovMatchAllQuery(targetRepository, documentTypes);
            searchRequest.setAzzimovQuery(azzimovMatchAllQuery);
            List<AzzimovSuggestor> azzimovSuggestors = new ArrayList<>();
            azzimovSuggestors.add(azzimovSuggestor);
            searchRequest.setAzzimovSuggestorList(azzimovSuggestors);
            azzimovSearchRequestList.add(searchRequest);
        }
        // Execute the query and retrieve results
        azzimovMultiSearchRequest.setAzzimovSearchRequestList(azzimovSearchRequestList);
        com.azzimov.search.common.responses.AzzimovMultiSearchResponse azzimovMultiSearchResponse = searchExecutorService
                .getExecutorService().performSearchRequest(azzimovMultiSearchRequest);
        Iterator<com.azzimov.search.common.requests.AzzimovSearchRequest> searchRequestIterator = azzimovMultiSearchRequest
                .getAzzimovSearchRequestList().iterator();
        List<AzzimovSearchResponse> azzimovSearchResponseList = new ArrayList<>();
        AzzimovSearchResponse azzimovSearchResponseFinal = new AzzimovSearchResponse();
        AzzimovSuggestionResponse azzimovSuggestResponse = new AzzimovSuggestionResponse();
        List<String> suggestionlList = new ArrayList<>();
        for (com.azzimov.search.common.responses.AzzimovSearchResponse azzimovSearchResponse :
                azzimovMultiSearchResponse.getAzzimovSearchResponseList()) {
            AzzimovSearchResponseBuilder azzimovSearchResponseBuilder = new AzzimovSearchResponseBuilder(
                    azzimovSearchResponse,
                    searchRequestIterator.next());
            AzzimovSearchResponse searchResponse = azzimovSearchResponseBuilder.build();
            if (searchResponse.getAzzimovSuggestionResponse() != null &&
                    !searchResponse.getAzzimovSuggestionResponse().getSuggestions().isEmpty())
                suggestionlList.addAll(searchResponse.getAzzimovSuggestionResponse().getSuggestions());
        }
        azzimovSuggestResponse.setSuggestions(suggestionlList);
        azzimovSearchResponseFinal.setAzzimovSuggestionResponse(azzimovSuggestResponse);
        azzimovSearchResponseList.add(azzimovSearchResponseFinal);
        return azzimovSearchResponseList;
    }
}
