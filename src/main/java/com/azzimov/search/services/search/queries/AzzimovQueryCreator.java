package com.azzimov.search.services.search.queries;

import com.azzimov.search.common.query.AzzimovQuery;
import com.azzimov.search.services.search.params.AzzimovParameters;

/**
 * Created by prasad on 1/11/18.
 * AzzimovQueryCreator provides an interface definition to create concrete Azzimov Queries to Azzimov Search
 */
public abstract class AzzimovQueryCreator <Parameters extends AzzimovParameters,
        QueryI extends  AzzimovQuery, QueryO extends AzzimovQuery> {
    public abstract QueryO createAzzimovQuery(Parameters azzimovParameters, QueryI query);
}
