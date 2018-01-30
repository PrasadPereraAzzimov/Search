package com.azzimov.search.services.search.filters;

import com.azzimov.search.common.query.AzzimovQuery;
import com.azzimov.search.services.search.params.AzzimovParameters;

/**
 * Created by prasad on 1/17/18.
 * AzzimovFilterCreator provides an interface definition to create concrete Azzimov filter queries to Azzimov Search
 */
public abstract class AzzimovFilterCreator <Parameters extends AzzimovParameters,
        AzzimovQI extends AzzimovQuery, AzzimovQO extends AzzimovQuery> {
    public abstract AzzimovQO createAzzimovQuery(Parameters azzimovParameters, AzzimovQI azzimovQOs);
}
