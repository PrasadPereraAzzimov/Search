package com.azzimov.search.services.search.executors.product;

import com.azzimov.search.common.aggregators.AzzimovAggregator;
import com.azzimov.search.common.dto.communications.responses.search.AzzimovSearchResponse;
import com.azzimov.search.common.query.AzzimovBooleanQuery;
import com.azzimov.search.common.query.AzzimovFunctionScoreQuery;
import com.azzimov.search.common.sorters.AzzimovSorter;
import com.azzimov.search.common.util.config.ConfigurationHandler;
import com.azzimov.search.services.search.aggregators.product.AzzimovProductSearchAggregatorCreator;
import com.azzimov.search.services.search.executors.AzzimovSearchExecutor;
import com.azzimov.search.services.search.executors.SearchExecutorService;
import com.azzimov.search.services.search.filters.product.AzzimovProductSearchAttributeFilterCreator;
import com.azzimov.search.services.search.filters.product.AzzimovProductSearchRefinementFilterCreator;
import com.azzimov.search.services.search.params.product.AzzimovSearchParameters;
import com.azzimov.search.services.search.queries.product.AzzimovProductSearchExactQueryCreator;
import com.azzimov.search.services.search.queries.product.AzzimovProductSearchQueryCreator;
import com.azzimov.search.services.search.queries.product.AzzimovProductSearchScoreQueryCreator;
import com.azzimov.search.services.search.reponses.AzzimovSearchResponseBuilder;
import com.azzimov.search.services.search.sorters.product.AzzimovProductSearchSorterCreator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by prasad on 1/30/18.
 * AzzimovProductSearchExecutor provides product search query generation and execution
 */
public class AzzimovProductSearchExecutor extends AzzimovSearchExecutor {
    private ConfigurationHandler configurationHandler;
    private SearchExecutorService searchExecutorService;

    public AzzimovProductSearchExecutor(ConfigurationHandler configurationHandler,
                                        SearchExecutorService searchExecutorService) {
        this.configurationHandler = configurationHandler;
        this.searchExecutorService = searchExecutorService;
    }

    @Override
    public AzzimovSearchResponse search(AzzimovSearchParameters azzimovSearchParameters)
            throws IllegalAccessException, IOException, InstantiationException {
        AzzimovProductSearchQueryCreator azzimovProductSearchQueryCreator =
                new AzzimovProductSearchQueryCreator(configurationHandler);
        AzzimovBooleanQuery azzimovBooleanQuery =
                azzimovProductSearchQueryCreator.createAzzimovQuery(azzimovSearchParameters, null);
        com.azzimov.search.common.requests.AzzimovSearchRequest searchRequest =
                new com.azzimov.search.common.requests.AzzimovSearchRequest();

        AzzimovProductSearchAttributeFilterCreator azzimovProductSearchFilterCreator =
                new AzzimovProductSearchAttributeFilterCreator(configurationHandler);
        azzimovBooleanQuery = azzimovProductSearchFilterCreator
                .createAzzimovQuery(azzimovSearchParameters, azzimovBooleanQuery);
        AzzimovProductSearchRefinementFilterCreator productSearchRefinementFilterCreator =
                new AzzimovProductSearchRefinementFilterCreator(configurationHandler);
        azzimovBooleanQuery = productSearchRefinementFilterCreator.createAzzimovQuery(azzimovSearchParameters,
                azzimovBooleanQuery);
        AzzimovProductSearchExactQueryCreator azzimovProductSearchExactQueryCreator =
                new AzzimovProductSearchExactQueryCreator(configurationHandler);
        AzzimovFunctionScoreQuery azzimovFunctionScoreQuery =
                azzimovProductSearchExactQueryCreator
                        .createAzzimovQuery(azzimovSearchParameters, azzimovBooleanQuery);
        AzzimovProductSearchScoreQueryCreator azzimovSearchScoreAssimilatorCreator =
                new AzzimovProductSearchScoreQueryCreator(configurationHandler);
        azzimovFunctionScoreQuery = azzimovSearchScoreAssimilatorCreator
                .createAzzimovQuery(azzimovSearchParameters, azzimovFunctionScoreQuery);
        searchRequest.setAzzimovQuery(azzimovFunctionScoreQuery);

        AzzimovProductSearchSorterCreator azzimovProductSearchSorterCreator =
                new AzzimovProductSearchSorterCreator(configurationHandler);
        List<AzzimovSorter> azzimovSorterList = new ArrayList<>();
        azzimovSorterList = azzimovProductSearchSorterCreator
                .createAzzimovQuery(azzimovSearchParameters, azzimovSorterList);
        searchRequest.setAzzimovSorter(azzimovSorterList);

        AzzimovProductSearchAggregatorCreator azzimovProductSearchAggregatorCreator =
                new AzzimovProductSearchAggregatorCreator(configurationHandler);
        List<AzzimovAggregator> termAggregatorList = azzimovProductSearchAggregatorCreator
                .createAzzimovQuery(azzimovSearchParameters, null);
        searchRequest.setAzzimovAggregator(termAggregatorList);

        com.azzimov.search.common.responses.AzzimovSearchResponse azzimovSearchResponse = searchExecutorService
                .getExecutorService().performSearchRequest(searchRequest);
        AzzimovSearchResponseBuilder azzimovSearchResponseBuilder = new AzzimovSearchResponseBuilder(
                azzimovSearchResponse,
                searchRequest);
        return azzimovSearchResponseBuilder.build();
    }
}
