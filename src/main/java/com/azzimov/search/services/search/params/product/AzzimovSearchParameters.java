package com.azzimov.search.services.search.params.product;

import com.azzimov.search.common.dto.communications.requests.search.AzzimovSearchRequest;
import com.azzimov.search.services.search.params.AzzimovParameters;

import java.util.Map;

/**
 * Created by prasad on 1/10/18.
 * AzzimovSearchParameters provides the parameter dto which will be processed by search
 */
public class AzzimovSearchParameters implements AzzimovParameters {
    private AzzimovSearchRequest azzimovSearchRequest;
    private Map<String, String> targetRepositories;


    public AzzimovSearchRequest getAzzimovSearchRequest() {
        return azzimovSearchRequest;
    }

    public void setAzzimovSearchRequest(AzzimovSearchRequest azzimovSearchRequest) {
        this.azzimovSearchRequest = azzimovSearchRequest;
    }

    public Map<String, String> getTargetRepositories() {
        return targetRepositories;
    }

    public void setTargetRepositories(Map<String, String> targetRepositories) {
        this.targetRepositories = targetRepositories;
    }
}
