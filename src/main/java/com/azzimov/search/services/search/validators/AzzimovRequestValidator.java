package com.azzimov.search.services.search.validators;

import com.azzimov.search.common.dto.communications.requests.AzzimovRequest;
import com.azzimov.search.services.search.params.AzzimovParameters;

/**
 * Created by prasad on 1/10/18.
 * AzzimovRequestValidator validate the request parameters
 */
public interface AzzimovRequestValidator <R extends AzzimovRequest, P extends AzzimovParameters> {
    P validateRequest(R azzimovRequest);
}
