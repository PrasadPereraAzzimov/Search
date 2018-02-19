package com.azzimov.search.services.search.executors;

import com.azzimov.search.common.dto.communications.responses.search.AzzimovSearchResponse;
import com.azzimov.search.services.search.params.product.AzzimovSearchParameters;
import java.io.IOException;
import java.util.List;

/**
 * Created by prasad on 2/12/18.
 * AzzimovAggregateExecutor provides an interface to implement various aggregations required in Azzimov search
 */
public abstract class AzzimovAggregateExecutor {
    public abstract List<AzzimovSearchResponse> aggregate(List<AzzimovSearchParameters> azzimovSearchParameters)
            throws IllegalAccessException, IOException, InstantiationException;
}
