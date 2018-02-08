package com.azzimov.search.services.search.sorters.product;

import com.azzimov.search.common.dto.SortMode;
import com.azzimov.search.common.dto.externals.Product;
import com.azzimov.search.common.query.AzzimovFunctionScoreQuery;
import com.azzimov.search.common.query.AzzimovQuery;
import com.azzimov.search.common.sorters.AzzimovSorter;
import com.azzimov.search.common.util.config.ConfigurationHandler;
import com.azzimov.search.services.search.learn.LearnStatModelService;
import com.azzimov.search.services.search.params.product.AzzimovSearchParameters;
import com.azzimov.search.services.search.sorters.AzzimovSorterCreator;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by prasad on 1/25/18.
 * AzzimovProductSearchSorterCreator provides the Azzimov Product related search sorters
 */
public class AzzimovProductSearchSorterCreator extends AzzimovSorterCreator<AzzimovSearchParameters,
        AzzimovSorter, AzzimovSorter, AzzimovQuery, AzzimovFunctionScoreQuery> {
    private ConfigurationHandler configurationHandler;
    private LearnStatModelService learnStatModelService;

    public AzzimovProductSearchSorterCreator(ConfigurationHandler configurationHandler,
                                             LearnStatModelService learnStatModelService) {
        this.configurationHandler = configurationHandler;
        this.learnStatModelService = learnStatModelService;
    }

    @Override
    public List<AzzimovSorter> createAzzimovSorter(AzzimovSearchParameters azzimovParameters,
                                                   List<AzzimovSorter> azzimovSorterList) {
        // In this case, we will build an azzimov query based on our search parameter query terms
        // The field specific query we build here is a concrete implementation of our product search keyword query for
        // product documents
        // For the Azzimov Product Search query creator, we will not consider the input query azzimov query as this
        // is the base/core of product query
        // Retrieve the language field for the query language
        String sortFieldType = azzimovParameters.getAzzimovSearchRequest()
                .getAzzimovSearchRequestParameters()
                .getAzzimovSearchSortRequestParameters()
                .getField();
        // we set the field value to upper case as our sort enum is defined with uppercase keywords
        SortMode sortMode = SortMode.valueOf(sortFieldType.toUpperCase());
        switch (sortMode) {
            case TITLE:
                AzzimovProductSearchTitleSorter azzimovProductSearchTitleSorter =
                        new AzzimovProductSearchTitleSorter(configurationHandler);
                azzimovSorterList.addAll(
                        azzimovProductSearchTitleSorter.createAzzimovSorter(azzimovParameters, azzimovSorterList));
                break;
            case ADDED:
                AzzimovProductSearchTimeSorter azzimovProductSearchTimeSorter =
                        new AzzimovProductSearchTimeSorter(configurationHandler, Product.PRODUCT_DATE_CREATED);
                azzimovSorterList.addAll(
                azzimovProductSearchTimeSorter.createAzzimovSorter(azzimovParameters, azzimovSorterList));
                break;
            case PRICE:
                // price relevance is still not defined properly ?
                break;
            case UPDATED:
                azzimovProductSearchTimeSorter =
                        new AzzimovProductSearchTimeSorter(configurationHandler, Product.PRODUCT_DATE_CREATED);
                azzimovSorterList.addAll(
                azzimovProductSearchTimeSorter.createAzzimovSorter(azzimovParameters, azzimovSorterList));
                break;
        }
        return azzimovSorterList;
    }

    @Override
    public List<AzzimovFunctionScoreQuery> createAzzimovQuery(AzzimovSearchParameters azzimovParameters,
                                                              List<AzzimovQuery> azzimovQueryList) {
        String sortFieldType = azzimovParameters.getAzzimovSearchRequest()
                .getAzzimovSearchRequestParameters()
                .getAzzimovSearchSortRequestParameters()
                .getField();
        // we set the field value to upper case as our sort enum is defined with uppercase keywords
        SortMode sortMode = SortMode.valueOf(sortFieldType.toUpperCase());
        List<AzzimovFunctionScoreQuery> azzimovFunctionScoreQueryList = new ArrayList<>();
        switch (sortMode) {
            case RELEVANCE:
                AzzimovProductSearchCentroidSorter azzimovProductSearchCentroidSorter =
                        new AzzimovProductSearchCentroidSorter(configurationHandler, learnStatModelService);
               azzimovFunctionScoreQueryList.addAll(azzimovProductSearchCentroidSorter
                       .createAzzimovQuery(azzimovParameters, azzimovQueryList));
                break;
        }
        return azzimovFunctionScoreQueryList;
    }

    public ConfigurationHandler getConfigurationHandler() {
        return configurationHandler;
    }

    public LearnStatModelService getLearnStatModelService() {
        return learnStatModelService;
    }
}
