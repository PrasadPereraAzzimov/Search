package com.azzimov.search.services.search.executors.feedback;

import com.azzimov.search.common.dto.communications.requests.AzzimovRequestFilter;
import com.azzimov.search.common.dto.communications.requests.search.AzzimovSearchRequest;
import com.azzimov.search.common.dto.communications.responses.search.AzzimovSearchResponse;
import com.azzimov.search.common.dto.externals.AzzimovRequestRefinement;
import com.azzimov.search.common.query.AzzimovBooleanQuery;
import com.azzimov.search.common.util.config.ConfigurationHandler;
import com.azzimov.search.services.search.executors.AzzimovSearchExecutor;
import com.azzimov.search.services.search.executors.SearchExecutorService;
import com.azzimov.search.services.search.filters.product.AzzimovProductSearchAttributeFilterCreator;
import com.azzimov.search.services.search.filters.product.AzzimovProductSearchRefinementFilterCreator;
import com.azzimov.search.services.search.params.product.AzzimovSearchParameters;
import com.azzimov.search.services.search.queries.product.AzzimovProductSearchQueryCreator;
import com.azzimov.search.services.search.reponses.AzzimovSearchResponseBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by prasad on 1/31/18.
 * AzzimovFeedbackQueryExecutor provides query executor that required in guidance feedbacks
 */
public class AzzimovFeedbackQueryExecutor extends AzzimovSearchExecutor {
    private ConfigurationHandler configurationHandler;
    private SearchExecutorService searchExecutorService;

    public AzzimovFeedbackQueryExecutor(ConfigurationHandler configurationHandler,
                                        SearchExecutorService searchExecutorService) {
        this.configurationHandler = configurationHandler;
        this.searchExecutorService = searchExecutorService;
    }

    @Override
    public List<AzzimovSearchResponse> search(List<AzzimovSearchParameters> azzimovSearchParametersList)
            throws IllegalAccessException, IOException, InstantiationException {
        // First, create the query string query we use in search
        // Create the main/core query of the search first
        List<AzzimovSearchResponse> azzimovSearchResponseList = new ArrayList<>();
        for (AzzimovSearchParameters azzimovSearchParameters : azzimovSearchParametersList) {
            AzzimovSearchRequest azzimovSearchRequest = azzimovSearchParameters.getAzzimovSearchRequest();
            // First get query match count
            AzzimovProductSearchQueryCreator azzimovProductSearchQueryCreator =
                    new AzzimovProductSearchQueryCreator(configurationHandler);
            AzzimovBooleanQuery azzimovBooleanQuery =
                    azzimovProductSearchQueryCreator.createAzzimovQuery(azzimovSearchParameters, null);

            com.azzimov.search.common.requests.AzzimovSearchRequest searchRequest =
                    new com.azzimov.search.common.requests.AzzimovSearchRequest();

            searchRequest.setAzzimovQuery(azzimovBooleanQuery);
            // Execute the query and retrieve results
            com.azzimov.search.common.responses.AzzimovSearchResponse azzimovSearchResponse = searchExecutorService
                    .getExecutorService().performSearchRequest(searchRequest);

            // Build the Azzimvo Search response and return it
            AzzimovSearchResponseBuilder azzimovSearchResponseBuilder = new AzzimovSearchResponseBuilder(
                    azzimovSearchResponse,
                    searchRequest);
            azzimovSearchResponseList.add(azzimovSearchResponseBuilder.build());

            // First go through AzzimovRequestFilter types
            for (AzzimovRequestFilter azzimovRequestFilter :
                    azzimovSearchRequest.getAzzimovSearchRequestParameters().getAzzimovRequestFilters()) {
                List<AzzimovRequestFilter> azzimovRequestFilterList = new ArrayList<>();
                azzimovRequestFilterList.add(azzimovRequestFilter);
                azzimovSearchParameters.getAzzimovSearchRequest()
                        .getAzzimovSearchRequestParameters().setAzzimovRequestFilters(azzimovRequestFilterList);
                azzimovProductSearchQueryCreator =
                        new AzzimovProductSearchQueryCreator(configurationHandler);
                azzimovBooleanQuery =
                        azzimovProductSearchQueryCreator.createAzzimovQuery(azzimovSearchParameters, null);

                // Create attribute related filters on the search if the parameters contain attribute filters
                AzzimovProductSearchAttributeFilterCreator azzimovProductSearchFilterCreator =
                        new AzzimovProductSearchAttributeFilterCreator(configurationHandler);
                azzimovBooleanQuery = azzimovProductSearchFilterCreator
                        .createAzzimovQuery(azzimovSearchParameters, azzimovBooleanQuery);

                searchRequest = new com.azzimov.search.common.requests.AzzimovSearchRequest();

                searchRequest.setAzzimovQuery(azzimovBooleanQuery);
                // Execute the query and retrieve results
                azzimovSearchResponse = searchExecutorService.getExecutorService().performSearchRequest(searchRequest);

                // Build the Azzimvo Search response and return it
                azzimovSearchResponseBuilder = new AzzimovSearchResponseBuilder(azzimovSearchResponse, searchRequest);
                azzimovSearchResponseList.add(azzimovSearchResponseBuilder.build());
            }
            // Then go through AzzimovRequestFilter types
            AzzimovRequestRefinement azzimovRequestRefinement =
                    azzimovSearchParameters.getAzzimovSearchRequest()
                            .getAzzimovSearchRequestParameters().getAzzimovRequestRefinement();
            while (azzimovRequestRefinement != null) {
                AzzimovRequestRefinement azzimovRequestRefinementMinor = new AzzimovRequestRefinement();
                azzimovRequestRefinementMinor.setLabel(azzimovRequestRefinement.getLabel());
                azzimovRequestRefinementMinor.setValue(azzimovRequestRefinement.getValue());
                azzimovSearchParameters.getAzzimovSearchRequest().getAzzimovSearchRequestParameters()
                        .setAzzimovRequestRefinement(azzimovRequestRefinementMinor);

                azzimovProductSearchQueryCreator = new AzzimovProductSearchQueryCreator(configurationHandler);
                azzimovBooleanQuery = azzimovProductSearchQueryCreator.createAzzimovQuery(azzimovSearchParameters, null);

                // Create category/refinement related filters on the search if the parameters contain attribute filters
                AzzimovProductSearchRefinementFilterCreator productSearchRefinementFilterCreator =
                        new AzzimovProductSearchRefinementFilterCreator(configurationHandler);
                azzimovBooleanQuery = productSearchRefinementFilterCreator.createAzzimovQuery(azzimovSearchParameters,
                        azzimovBooleanQuery);

                searchRequest = new com.azzimov.search.common.requests.AzzimovSearchRequest();
                searchRequest.setAzzimovQuery(azzimovBooleanQuery);
                // Execute the query and retrieve results
                azzimovSearchResponse = searchExecutorService.getExecutorService().performSearchRequest(searchRequest);

                // Build the Azzimvo Search response and return it
                azzimovSearchResponseBuilder = new AzzimovSearchResponseBuilder(azzimovSearchResponse, searchRequest);
                azzimovSearchResponseList.add(azzimovSearchResponseBuilder.build());
                azzimovRequestRefinement = azzimovRequestRefinement.getChildAzzimovRequestRefinement();
            }
        }
        return azzimovSearchResponseList;
    }
}
