package com.azzimov.search.services.search.filters;

import com.azzimov.search.common.query.AzzimovQuery;
import com.azzimov.search.services.search.params.AzzimovParameters;
import java.util.List;

/**
 * Created by prasad on 1/17/18.
 */
public abstract class AzzimovFilterCreator <Parameters extends AzzimovParameters,
        AzzimovQI extends AzzimovQuery, AzzimovQO extends AzzimovQuery> {
    abstract AzzimovQO createAzzimovQuery(Parameters azzimovParameters, AzzimovQI azzimovQOs);
}
