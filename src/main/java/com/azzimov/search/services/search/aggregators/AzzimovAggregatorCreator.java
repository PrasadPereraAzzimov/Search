package com.azzimov.search.services.search.aggregators;

import com.azzimov.search.common.aggregators.AzzimovAggregator;
import com.azzimov.search.services.search.params.AzzimovParameters;

import java.util.List;

/**
 * Created by prasad on 1/16/18.
 */
public abstract class AzzimovAggregatorCreator <Parameters extends AzzimovParameters,
        AggregatorI extends AzzimovAggregator, AggregatorO extends AzzimovAggregator> {
    abstract List<AggregatorO> createAzzimovQuery(Parameters azzimovParameters, List<AggregatorI> aggregatorI);
}
