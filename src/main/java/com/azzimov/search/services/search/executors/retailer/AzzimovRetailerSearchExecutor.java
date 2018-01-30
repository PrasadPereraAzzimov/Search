package com.azzimov.search.services.search.executors.retailer;

import com.azzimov.search.common.dto.communications.responses.search.AzzimovSearchResponse;
import com.azzimov.search.common.util.config.ConfigurationHandler;
import com.azzimov.search.services.search.executors.AzzimovSearchExecutor;
import com.azzimov.search.services.search.executors.SearchExecutorService;
import com.azzimov.search.services.search.params.product.AzzimovSearchParameters;

import java.io.IOException;

/**
 * Created by prasad on 1/30/18.
 * AzzimovRetailerSearchExecutor provides retailer type search query building & execution
 */
public class AzzimovRetailerSearchExecutor extends AzzimovSearchExecutor {
    private ConfigurationHandler configurationHandler;
    private SearchExecutorService searchExecutorService;

    public AzzimovRetailerSearchExecutor(ConfigurationHandler configurationHandler,
                                        SearchExecutorService searchExecutorService) {
        this.configurationHandler = configurationHandler;
        this.searchExecutorService = searchExecutorService;
    }

    @Override
    public AzzimovSearchResponse search(AzzimovSearchParameters azzimovSearchParameters)
            throws IllegalAccessException, IOException, InstantiationException {
        return new AzzimovSearchResponse();
    }
}
