package com.azzimov.search.services.search.aggregators;

import com.azzimov.search.common.aggregators.AzzimovAggregator;
import com.azzimov.search.services.search.params.AzzimovParameters;

import java.util.List;

/**
 * Created by prasad on 1/16/18.
 * AzzimovAggregatorCreator provides an interface definition to create concrete Azzimov Queries to Azzimov Search
 */
public abstract class AzzimovAggregatorCreator <Parameters extends AzzimovParameters,
        AggregatorI, AggregatorO extends AzzimovAggregator> {
    public abstract List<AggregatorO> createAzzimovQuery(Parameters azzimovParameters, AggregatorI aggregatorI);
}
