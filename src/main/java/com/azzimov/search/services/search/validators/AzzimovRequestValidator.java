package com.azzimov.search.services.search.validators;

import com.azzimov.search.common.dto.communications.requests.AzzimovRequest;
import com.azzimov.search.services.search.params.AzzimovParameters;

/**
 * Created by prasad on 1/10/18.
 */
public interface AzzimovRequestValidator <R extends AzzimovRequest, P extends AzzimovParameters> {
    public P validateRequest(R azzimovRequest);
}
