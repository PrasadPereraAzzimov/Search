package com.azzimov.search.services.search.queries;

import com.azzimov.search.common.dto.externals.ExternalDTO;
import com.azzimov.search.common.query.AzzimovQuery;
import com.azzimov.search.services.search.params.AzzimovParameters;

/**
 * Created by prasad on 1/11/18.
 */
public abstract class AzzimovQueryCreator <Parameters extends AzzimovParameters,
        QueryI extends  AzzimovQuery, QueryO extends AzzimovQuery> {
    abstract QueryO createAzzimovQuery(Parameters azzimovParameters, QueryI query);

    public static String retrieveFieldPath(String... path) {
        String fieldPath = "";
        for (String p : path) {
            fieldPath += (p + ExternalDTO.EXTERNAL_FIELD_DELIMITER);
        }
        return fieldPath.substring(0, fieldPath.length() - 1);
    }
}
