package com.azzimov.search.services.search.executors;

import com.azzimov.search.common.dto.communications.responses.search.AzzimovSearchResponse;
import com.azzimov.search.services.search.params.product.AzzimovSearchParameters;

import java.io.IOException;
import java.util.List;

/**
 * Created by prasad on 1/30/18.
 * AzzimovSearchExecutor provides the method search to implement concrete type search query generation and executing of
 *  search. A different search implementation will provide different search on product/store etc.
 */
public abstract class AzzimovSearchExecutor {
    public abstract List<AzzimovSearchResponse> search(List<AzzimovSearchParameters> azzimovSearchParameters)
            throws IllegalAccessException, IOException, InstantiationException;
}
