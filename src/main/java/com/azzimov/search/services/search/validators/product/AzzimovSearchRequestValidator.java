package com.azzimov.search.services.search.validators.product;

import com.azzimov.search.common.dto.communications.requests.AzzimovUserRequestParameters;
import com.azzimov.search.common.dto.communications.requests.search.AzzimovSearchRequest;
import com.azzimov.search.common.dto.communications.requests.search.AzzimovSearchRequestParameters;
import com.azzimov.search.common.util.config.ConfigurationHandler;
import com.azzimov.search.common.util.config.SearchConfiguration;
import com.azzimov.search.listeners.ConfigListener;
import com.azzimov.search.services.search.params.product.AzzimovSearchParameters;
import com.azzimov.search.services.search.validators.AzzimovRequestValidator;
import java.security.InvalidParameterException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by prasad on 1/10/18.
 * AzzimovSearchRequestValidator validate Azzimov Search Request parameters and returns Azzimov Search Parameters
 */
public class AzzimovSearchRequestValidator implements
        AzzimovRequestValidator<AzzimovSearchRequest, AzzimovSearchParameters> {
    private ConfigurationHandler configurationHandler;

    public AzzimovSearchRequestValidator(ConfigurationHandler configurationHandler) {
        this.configurationHandler = configurationHandler;
    }

    @Override
    public AzzimovSearchParameters validateRequest(AzzimovSearchRequest azzimovSearchRequest) throws InvalidParameterException{
        List<String> languages = configurationHandler.getStringConfigList(SearchConfiguration.SEARCH_LANGUAGES);
        List<String> documents = configurationHandler.getStringConfigList(SearchConfiguration.SEARCH_DOCUMENT_TYPE);
        List<Object> targetRepositoriesConfigs = configurationHandler
                .getObjectConfigList(SearchConfiguration.SEARCH_DOC_TARGET_INDEXES);
        Map<String, String> repositories = ConfigListener.retrieveTargetRepositoriesforDocuments(targetRepositoriesConfigs,
                configurationHandler);
        Map<String, String> targetRepositories = new HashMap<>();
        // check if the request contains required/must parameters
        if (azzimovSearchRequest.getAzzimovUserRequestParameters() != null) {
            AzzimovUserRequestParameters azzimovUserRequestParameters =
                    azzimovSearchRequest.getAzzimovUserRequestParameters();
            if (azzimovUserRequestParameters.getSessionId() == null ||
                    azzimovUserRequestParameters.getSessionId().isEmpty())
                throw new InvalidParameterException("Missing required request field:" + "session_id");
            if (azzimovUserRequestParameters.getRequestId() == null ||
                    azzimovUserRequestParameters.getRequestId().isEmpty())
                throw new InvalidParameterException("Missing required request field:" + "request_id");
            if (azzimovUserRequestParameters.getMemberId() == null ||
                    azzimovUserRequestParameters.getMemberId().isEmpty())
                throw new InvalidParameterException("Missing required request field:" + "member_id");
            if (azzimovUserRequestParameters.getUserType() == null ||
                    azzimovUserRequestParameters.getUserType().isEmpty())
                throw new InvalidParameterException("Missing required request field:" + "user_type");
        } else {
            throw new InvalidParameterException("Missing required request field:" + "user_parameters");
        }

        if (azzimovSearchRequest.getAzzimovSearchRequestParameters() != null) {
            AzzimovSearchRequestParameters azzimovSearchRequestParameters =
                    azzimovSearchRequest.getAzzimovSearchRequestParameters();
            if (azzimovSearchRequestParameters.getResultOffset() < 0 ||
                    azzimovSearchRequestParameters.getResultsPerPage() > 1000)
                throw new InvalidParameterException("Invalid request parameter:" + "results_per_page");
            if (azzimovSearchRequestParameters.getResultOffset() < 0 ||
                    azzimovSearchRequestParameters.getResultOffset() > 1000)
                throw new InvalidParameterException("Invalid request parameter:" + "results_offset");
            if (azzimovSearchRequestParameters.getLanguage() == null ||
                    !languages.contains(azzimovSearchRequestParameters.getLanguage().getLanguageCode().getValue()))
                throw new InvalidParameterException("Invalid request parameter:" + "language");
            if (azzimovSearchRequestParameters.getDocumentTypes() == null ||
                    !documents.containsAll(azzimovSearchRequestParameters.getDocumentTypes()))
                throw new InvalidParameterException("Invalid request parameter:" + "types");
        } else {
            throw new InvalidParameterException("Missing required request field:" + "search_parameters");
        }
        // If we reach here, that means parameters are validated!
        AzzimovSearchParameters azzimovSearchParameters = new AzzimovSearchParameters();
        azzimovSearchParameters.setAzzimovSearchRequest(azzimovSearchRequest);
        // Add repositories targetted by query to search query request
        for (String documentType : azzimovSearchRequest.getAzzimovSearchRequestParameters().getDocumentTypes()) {
            targetRepositories.put(documentType, repositories.get(documentType));
        }
        azzimovSearchParameters.setTargetRepositories(targetRepositories);
        return azzimovSearchParameters;
    }
}
