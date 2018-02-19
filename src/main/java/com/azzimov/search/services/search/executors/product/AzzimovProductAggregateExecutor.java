package com.azzimov.search.services.search.executors.product;

import com.azzimov.search.common.aggregators.AzzimovAggregator;
import com.azzimov.search.common.dto.communications.requests.AzzimovRequestFilter;
import com.azzimov.search.common.dto.communications.responses.search.AzzimovSearchResponse;
import com.azzimov.search.common.dto.externals.Guidance;
import com.azzimov.search.common.dto.externals.GuidanceFilter;
import com.azzimov.search.common.query.AzzimovBooleanQuery;
import com.azzimov.search.common.requests.AzzimovMultiSearchRequest;
import com.azzimov.search.common.responses.AzzimovMultiSearchResponse;
import com.azzimov.search.common.text.AzzimovTextProcessor;
import com.azzimov.search.common.util.config.ConfigurationHandler;
import com.azzimov.search.services.cache.AzzimovCacheManager;
import com.azzimov.search.services.search.aggregators.product.AzzimovProductAttributeAggregator;
import com.azzimov.search.services.search.aggregators.product.AzzimovProductSearchAggregatorCreator;
import com.azzimov.search.services.search.executors.AzzimovAggregateExecutor;
import com.azzimov.search.services.search.executors.SearchExecutorService;
import com.azzimov.search.services.search.filters.product.AzzimovProductSearchAttributeFilterCreator;
import com.azzimov.search.services.search.filters.product.AzzimovProductSearchRefinementFilterCreator;
import com.azzimov.search.services.search.params.product.AzzimovSearchParameters;
import com.azzimov.search.services.search.queries.product.AzzimovProductSearchQueryCreator;
import com.azzimov.search.services.search.reponses.AzzimovSearchResponseBuilder;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static com.azzimov.search.services.search.utils.AzzimovSearchDTOReplicator.replicate;

/**
 * Created by prasad on 2/12/18.
 * An implentation of AzzimovAggregateExecutor for product category/attribute aggregations
 */
public class AzzimovProductAggregateExecutor extends AzzimovAggregateExecutor {
    private ConfigurationHandler configurationHandler;
    private SearchExecutorService searchExecutorService;
    private AzzimovCacheManager azzimovCacheManager;

    public AzzimovProductAggregateExecutor(ConfigurationHandler configurationHandler,
                                           SearchExecutorService searchExecutorService,
                                           AzzimovCacheManager azzimovCacheManager) {
        this.configurationHandler = configurationHandler;
        this.searchExecutorService = searchExecutorService;
        this.azzimovCacheManager = azzimovCacheManager;
    }

    @Override
    public List<AzzimovSearchResponse> aggregate(List<AzzimovSearchParameters> azzimovSearchParametersList)
            throws IllegalAccessException, IOException, InstantiationException {
        List<AzzimovSearchResponse> azzimovSearchResponseList = new ArrayList<>();
        for (AzzimovSearchParameters azzimovSearchParameters : azzimovSearchParametersList) {
            AzzimovSearchResponse azzimovSearchResponse =
                    retrieveAndMergeAzzimovProductAggregations(azzimovSearchParameters);
            azzimovSearchResponseList.add(azzimovSearchResponse);
        }
        return azzimovSearchResponseList;
    }

    /**
     * Build different aggregations needed in multi attribute selection handling
     * @param azzimovSearchParameters   azzimov search parameters
     * @param includeList   include pattern lists for the specific aggregations
     * @return  azzimov search request containing aggregation
     * @throws IllegalAccessException
     * @throws IOException
     * @throws InstantiationException
     */
    private com.azzimov.search.common.requests.AzzimovSearchRequest buildAggregation(
            AzzimovSearchParameters azzimovSearchParameters,
            List<String> includeList)
            throws IllegalAccessException, IOException, InstantiationException {
        // Retrieve a copy of search parameters
        azzimovSearchParameters = replicate(azzimovSearchParameters);
        // set the size to zero for aggregation results
        azzimovSearchParameters.getAzzimovSearchRequest().
                getAzzimovSearchRequestParameters().setResultsPerPage(0);

        AzzimovProductSearchQueryCreator azzimovProductSearchQueryCreator =
                new AzzimovProductSearchQueryCreator(configurationHandler);
        AzzimovBooleanQuery azzimovBooleanQuery =
                azzimovProductSearchQueryCreator.createAzzimovQuery(azzimovSearchParameters, null);

        // Create attribute related filters on the search if the parameters contain attribute filters
        AzzimovProductSearchAttributeFilterCreator azzimovProductSearchFilterCreator =
                new AzzimovProductSearchAttributeFilterCreator(configurationHandler);
        azzimovBooleanQuery = azzimovProductSearchFilterCreator
                .createAzzimovQuery(azzimovSearchParameters, azzimovBooleanQuery);

        // Create category/refinement related filters on the search if the parameters contain attribute filters
        AzzimovProductSearchRefinementFilterCreator productSearchRefinementFilterCreator =
                new AzzimovProductSearchRefinementFilterCreator(configurationHandler);
        azzimovBooleanQuery = productSearchRefinementFilterCreator.createAzzimovQuery(azzimovSearchParameters,
                azzimovBooleanQuery);

        com.azzimov.search.common.requests.AzzimovSearchRequest searchRequest =
                new com.azzimov.search.common.requests.AzzimovSearchRequest();

        // For now, this is here but will be moved to guidance manager task
        if (includeList.isEmpty()) {
            AzzimovProductSearchAggregatorCreator azzimovProductSearchAggregatorCreator =
                    new AzzimovProductSearchAggregatorCreator(configurationHandler);
            List<AzzimovAggregator> termAggregatorList = azzimovProductSearchAggregatorCreator
                    .createAzzimovQuery(azzimovSearchParameters, null);
            searchRequest.setAzzimovAggregator(termAggregatorList);
        } else {
            AzzimovProductAttributeAggregator azzimovProductAttributeAggregator =
                    new AzzimovProductAttributeAggregator(configurationHandler);
            List<AzzimovAggregator> termAggregatorList = azzimovProductAttributeAggregator
                    .createAzzimovQuery(azzimovSearchParameters, includeList);
            searchRequest.setAzzimovAggregator(termAggregatorList);
        }
        searchRequest.setAzzimovQuery(azzimovBooleanQuery);
        return searchRequest;
    }


    private AzzimovSearchResponse retrieveAndMergeAzzimovProductAggregations(AzzimovSearchParameters azzimovSearchParameters)
            throws IllegalAccessException, IOException, InstantiationException {
        // First, group the filters based on label so we can apply them separately to the queries
        List<AzzimovRequestFilter> azzimovRequestFilterList = azzimovSearchParameters.getAzzimovSearchRequest()
        .getAzzimovSearchRequestParameters().getAzzimovRequestFilters();

        Map<String, List<AzzimovRequestFilter>> azzimovRequestFilerMap = new HashMap<>();
        for (AzzimovRequestFilter azzimovRequestFilter : azzimovRequestFilterList) {
            azzimovRequestFilerMap.put(azzimovRequestFilter.getLabel(), new ArrayList<>());
        }
        // Then we create opposing filters to the given label group so we can get aggregation of different facets
        for (Map.Entry<String, List<AzzimovRequestFilter>> entry : azzimovRequestFilerMap.entrySet()) {
            String label = entry.getKey();
            for (AzzimovRequestFilter azzimovRequestFilter : azzimovRequestFilterList) {
                if (!azzimovRequestFilter.getLabel().equals(label)) {
                    entry.getValue().add(azzimovRequestFilter);
                }
            }
        }
        // First perform aggregation based on all filters
        AzzimovMultiSearchRequest azzimovMultiSearchRequest = new AzzimovMultiSearchRequest();
        List<com.azzimov.search.common.requests.AzzimovSearchRequest> azzimovSearchRequestList = new ArrayList<>();
        com.azzimov.search.common.requests.AzzimovSearchRequest azzimovSearchRequest =
                buildAggregation(azzimovSearchParameters, new ArrayList<>());
        azzimovSearchRequestList.add(azzimovSearchRequest);
        // Retrieve aggregations based on these groups
        for (Map.Entry<String, List<AzzimovRequestFilter>> entry : azzimovRequestFilerMap.entrySet()) {
            String label = entry.getKey();
            List<AzzimovRequestFilter> azzimovRequestFilters = entry.getValue();
            List<String> includeList = new ArrayList<>();
            includeList.add(AzzimovTextProcessor.retrieveNonAlphaNumericEscapedText(label) + "::.*");
            AzzimovSearchParameters azzimovSearchParametersSub = replicate(azzimovSearchParameters);
            azzimovSearchParametersSub.getAzzimovSearchRequest()
                    .getAzzimovSearchRequestParameters().setAzzimovRequestFilters(azzimovRequestFilters);
            azzimovSearchRequest = buildAggregation(azzimovSearchParametersSub, includeList);
            azzimovSearchRequestList.add(azzimovSearchRequest);
        }
        azzimovMultiSearchRequest.setAzzimovSearchRequestList(azzimovSearchRequestList);

        // Peform multi search requests with different aggregations
        AzzimovMultiSearchResponse azzimovMultiSearchResponse = searchExecutorService.getExecutorService()
                .performSearchRequest(azzimovMultiSearchRequest);

        Iterator<com.azzimov.search.common.responses.AzzimovSearchResponse> azzimovSearchResponseIterator =
                azzimovMultiSearchResponse.getAzzimovSearchResponseList().iterator();
        AzzimovSearchResponseBuilder azzimovSearchResponseBuilder = new AzzimovSearchResponseBuilder(
                azzimovSearchResponseIterator.next(),
                null);
        AzzimovSearchResponse mainSearchResponse = azzimovSearchResponseBuilder.build();
        Guidance mainGuidance = mainSearchResponse.getAzzimovSearchResponseParameter().getGuidance();

        while (azzimovSearchResponseIterator.hasNext()) {
            com.azzimov.search.common.responses.AzzimovSearchResponse searchResponse = azzimovSearchResponseIterator.next();
            azzimovSearchResponseBuilder = new AzzimovSearchResponseBuilder(
                    searchResponse,
                    null);
            AzzimovSearchResponse azzimovSearchResponse = azzimovSearchResponseBuilder.build();
            Guidance guidance = azzimovSearchResponse.getAzzimovSearchResponseParameter().getGuidance();
            // Retrieve the specific filter
            if (!guidance.getGuidanceFilters().isEmpty()) {
                GuidanceFilter guidanceFilter = guidance.getGuidanceFilters().get(0);
                Iterator<GuidanceFilter> guidanceFilterIterator = mainGuidance.getGuidanceFilters().iterator();
                while (guidanceFilterIterator.hasNext()) {
                    GuidanceFilter guidanceFilterMain = guidanceFilterIterator.next();
                    if (guidanceFilterMain.getLabel().equals(guidanceFilter.getLabel())) {
                        guidanceFilterIterator.remove();
                    }
                }
                mainSearchResponse.getAzzimovSearchResponseParameter()
                        .getGuidance().getGuidanceFilters().add(guidanceFilter);
            }
        }
        return mainSearchResponse;
    }
}
