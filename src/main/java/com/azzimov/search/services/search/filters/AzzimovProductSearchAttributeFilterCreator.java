package com.azzimov.search.services.search.filters;

import com.azzimov.search.common.dto.LanguageCode;
import com.azzimov.search.common.dto.communications.requests.AzzimovRequestFilter;
import com.azzimov.search.common.dto.externals.Attribute;
import com.azzimov.search.common.dto.externals.Product;
import com.azzimov.search.common.query.AzzimovBooleanQuery;
import com.azzimov.search.common.query.AzzimovNestedQuery;
import com.azzimov.search.common.query.AzzimovQuery;
import com.azzimov.search.common.query.AzzimovTermTermQuery;
import com.azzimov.search.common.util.config.ConfigurationHandler;
import com.azzimov.search.common.util.config.SearchConfiguration;
import com.azzimov.search.services.search.params.AzzimovSearchParameters;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.azzimov.search.services.search.queries.AzzimovProductSearchExactQueryCreator.EXACT_FIELD;
import static com.azzimov.search.services.search.queries.AzzimovProductSearchExactQueryCreator.EXACT_FIELD_RAW;
import static com.azzimov.search.services.search.queries.AzzimovQueryCreator.retrieveFieldPath;

/**
 * Created by prasad on 1/17/18.
 */
public class AzzimovProductSearchAttributeFilterCreator extends AzzimovFilterCreator<AzzimovSearchParameters,
        AzzimovBooleanQuery, AzzimovBooleanQuery> {
    private ConfigurationHandler configurationHandler;

    public AzzimovProductSearchAttributeFilterCreator(ConfigurationHandler configurationHandler) {
        this.configurationHandler = configurationHandler;
    }

    @Override
    public AzzimovBooleanQuery createAzzimovQuery(AzzimovSearchParameters azzimovParameters,
                                                   AzzimovBooleanQuery azzimovQueries) {
        List<Object>  configList = configurationHandler.getObjectConfigList(SearchConfiguration.QUERY_BOOST_VALUES);
        Map<String, Integer> searchFieldBoosts = (Map<String, Integer>) configList.get(0);
        configList = configurationHandler.getObjectConfigList(SearchConfiguration.QUERY_MINIMUM_SHOULD_MATCH);
        Map<String, Integer> minimumFieldMatch = (Map<String, Integer>) configList.get(0);
        // In this case, we will build an azzimov query based on our search parameter query terms
        // The field specific query we build here is a concrete implementation of our product search keyword query for
        // product documents
        // For the Azzimov Product Search query creator, we will not consider the input query azzimov query as this
        // is the base/core of product query
        String query = azzimovParameters.getAzzimovSearchRequest().
                getAzzimovSearchRequestParameters().getQuery();
        Map<String, String> targetDocumentTypes = azzimovParameters.getTargetRepositories();
        // Here, for now, we expect one time of targets
        List<String> targetDocs = new ArrayList<>(targetDocumentTypes.keySet());
        // the target repository/index
        String targetRepository = targetDocumentTypes.values().iterator().next();
        // pre process the query as we want here
        AzzimovBooleanQuery azzimovBooleanQuery = new AzzimovBooleanQuery(targetRepository, targetDocs);
        // Retrieve the language field for the query language
        LanguageCode languageCode = azzimovParameters.getAzzimovSearchRequest()
                .getAzzimovSearchRequestParameters().getLanguage().getLanguageCode();

        List<AzzimovRequestFilter> azzimovRequestFilterList = azzimovParameters.
                getAzzimovSearchRequest().getAzzimovSearchRequestParameters().getAzzimovRequestFilters();
        if (azzimovRequestFilterList != null && !azzimovRequestFilterList.isEmpty()) {
            AzzimovBooleanQuery filterBooleanQuery = new AzzimovBooleanQuery(targetRepository, targetDocs);
            for (AzzimovRequestFilter azzimovRequestFilter : azzimovRequestFilterList) {
                AzzimovBooleanQuery attributeBooleanQuery = new AzzimovBooleanQuery(targetRepository, targetDocs);
                AzzimovTermTermQuery azzimovTermTermQuery = new AzzimovTermTermQuery(targetRepository,
                        retrieveFieldPath(Product.PRODUCT_ATTRIBUTES,
                                Attribute.ATTRIBUTE_LABEL,
                                LanguageCode.getLanguageField(languageCode),
                                EXACT_FIELD_RAW), targetDocs, azzimovRequestFilter.getLabel());
                attributeBooleanQuery.addMustQuery(azzimovTermTermQuery);
                if (azzimovRequestFilter.getFilterType().equals("text")) {
                    azzimovTermTermQuery = new AzzimovTermTermQuery(targetRepository,
                            retrieveFieldPath(Product.PRODUCT_ATTRIBUTES,
                                    Attribute.ATTRIBUTE_STRING_VALUE,
                                    LanguageCode.getLanguageField(languageCode),
                                    EXACT_FIELD_RAW), targetDocs, azzimovRequestFilter.getValue());
                    attributeBooleanQuery.addMustQuery(azzimovTermTermQuery);
                } else {
                    Double queryValue = Double.parseDouble(azzimovRequestFilter.getValue());
                    azzimovTermTermQuery = new AzzimovTermTermQuery(targetRepository,
                            retrieveFieldPath(Product.PRODUCT_ATTRIBUTES,
                                    Attribute.ATTRIBUTE_NUMBER_VALUE,
                                    "values_num"), targetDocs, queryValue);
                    attributeBooleanQuery.addMustQuery(azzimovTermTermQuery);
                }
                AzzimovNestedQuery azzimovNestedQuery = new AzzimovNestedQuery(targetRepository, targetDocs);
                azzimovNestedQuery.setPath(Product.PRODUCT_ATTRIBUTES);
                azzimovNestedQuery.setScoreMode(AzzimovNestedQuery.AzzimovNestedScoreMode.MAX);
                azzimovNestedQuery.setAzzimovQuery(attributeBooleanQuery);
                filterBooleanQuery.addShouldQuery(azzimovNestedQuery);
            }
            azzimovQueries.addFilterQuery(filterBooleanQuery);
        }
        return azzimovQueries;
    }
}
