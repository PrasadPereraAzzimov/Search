package com.azzimov.search.services.search.filters.product;

import com.azzimov.search.common.dto.LanguageCode;
import com.azzimov.search.common.dto.communications.requests.AzzimovRequestFilter;
import com.azzimov.search.common.dto.externals.Attribute;
import com.azzimov.search.common.dto.externals.Product;
import com.azzimov.search.common.query.AzzimovBooleanQuery;
import com.azzimov.search.common.query.AzzimovNestedQuery;
import com.azzimov.search.common.query.AzzimovTermTermQuery;
import com.azzimov.search.common.util.config.ConfigurationHandler;
import com.azzimov.search.services.search.filters.AzzimovFilterCreator;
import com.azzimov.search.services.search.params.product.AzzimovSearchParameters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.azzimov.search.services.search.utils.SearchFieldConstants.EXACT_FIELD_RAW;
import static com.azzimov.search.services.search.utils.SearchFieldConstants.VALUE_DATE;
import static com.azzimov.search.services.search.utils.SearchFieldConstants.VALUE_NUM;
import static com.azzimov.search.services.search.utils.SearchFieldConstants.VALUE_TEXT;
import static com.azzimov.search.services.search.utils.SearchFieldConstants.retrieveFieldPath;

/**
 * Created by prasad on 1/17/18.
 * AzzimovProductSearchAttributeFilterCreator provides product search filters according to the search
 * request parameters
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
        // In this case, we will build an azzimov query based on our search parameter query terms
        // The field specific query we build here is a concrete implementation of our product search keyword query for
        // product documents
        // For the Azzimov Product Search query creator, we will not consider the input query azzimov query as this
        // is the base/core of product query
        Map<String, String> targetDocumentTypes = azzimovParameters.getTargetRepositories();
        // Here, for now, we expect one time of targets
        List<String> targetDocs = new ArrayList<>();
        targetDocs.add(Product.PRODUCT_EXTERNAL_NAME);
        // the target repository/index
        String targetRepository = targetDocumentTypes.get(Product.PRODUCT_EXTERNAL_NAME);
        // Retrieve the language field for the query language
        LanguageCode languageCode = azzimovParameters.getAzzimovSearchRequest()
                .getAzzimovSearchRequestParameters().getLanguage().getLanguageCode();

        // Before creating attribute filters, we need to group filters based on filter labels.
        List<AzzimovRequestFilter> azzimovRequestFilterList = azzimovParameters.getAzzimovSearchRequest()
                .getAzzimovSearchRequestParameters().getAzzimovRequestFilters();
        Map<String, List<AzzimovRequestFilter>> azzimovRequestFilerMap = new HashMap<>();
        for (AzzimovRequestFilter azzimovRequestFilter : azzimovRequestFilterList) {
            if (!azzimovRequestFilerMap.containsKey(azzimovRequestFilter.getLabel())) {
                azzimovRequestFilerMap.put(azzimovRequestFilter.getLabel(), new ArrayList<>());
            }
            azzimovRequestFilerMap.get(azzimovRequestFilter.getLabel()).add(azzimovRequestFilter);
        }
        if (!azzimovRequestFilerMap.isEmpty()) {
            AzzimovBooleanQuery azzimovBooleanQueryMajor = new AzzimovBooleanQuery(targetRepository, targetDocs);
            for (Map.Entry<String, List<AzzimovRequestFilter>> azzimovRequestFilterEntry : azzimovRequestFilerMap.entrySet()) {
                AzzimovBooleanQuery filterBooleanQuery = new AzzimovBooleanQuery(targetRepository, targetDocs);
                for (AzzimovRequestFilter azzimovRequestFilter : azzimovRequestFilterEntry.getValue()) {
                    AzzimovBooleanQuery attributeBooleanQuery = new AzzimovBooleanQuery(targetRepository, targetDocs);
                    AzzimovTermTermQuery azzimovTermTermQuery = new AzzimovTermTermQuery(targetRepository,
                            retrieveFieldPath(Product.PRODUCT_ATTRIBUTES,
                                    Attribute.ATTRIBUTE_LABEL,
                                    LanguageCode.getLanguageField(languageCode),
                                    EXACT_FIELD_RAW), targetDocs, azzimovRequestFilter.getLabel());
                    attributeBooleanQuery.addMustQuery(azzimovTermTermQuery);
                    if (azzimovRequestFilter.getFilterType().equals(VALUE_TEXT)) {
                        azzimovTermTermQuery = new AzzimovTermTermQuery(targetRepository,
                                retrieveFieldPath(Product.PRODUCT_ATTRIBUTES,
                                        Attribute.ATTRIBUTE_STRING_VALUE,
                                        LanguageCode.getLanguageField(languageCode),
                                        EXACT_FIELD_RAW), targetDocs, azzimovRequestFilter.getValue());
                        attributeBooleanQuery.addMustQuery(azzimovTermTermQuery);
                    } else if (azzimovRequestFilter.getFilterType().equals(VALUE_DATE)) {
                        Double queryValue = Double.parseDouble(azzimovRequestFilter.getValue());
                        azzimovTermTermQuery = new AzzimovTermTermQuery(targetRepository,
                                retrieveFieldPath(Product.PRODUCT_ATTRIBUTES,
                                        Attribute.ATTRIBUTE_NUMBER_VALUE,
                                        VALUE_DATE), targetDocs, queryValue);
                        attributeBooleanQuery.addMustQuery(azzimovTermTermQuery);
                    } else {
                        Double queryValue = Double.parseDouble(azzimovRequestFilter.getValue());
                        azzimovTermTermQuery = new AzzimovTermTermQuery(targetRepository,
                                retrieveFieldPath(Product.PRODUCT_ATTRIBUTES,
                                        Attribute.ATTRIBUTE_NUMBER_VALUE,
                                        VALUE_NUM), targetDocs, queryValue);
                        attributeBooleanQuery.addMustQuery(azzimovTermTermQuery);
                    }
                    AzzimovNestedQuery azzimovNestedQuery = new AzzimovNestedQuery(targetRepository, targetDocs);
                    azzimovNestedQuery.setPath(Product.PRODUCT_ATTRIBUTES);
                    azzimovNestedQuery.setScoreMode(AzzimovNestedQuery.AzzimovNestedScoreMode.MAX);
                    azzimovNestedQuery.setAzzimovQuery(attributeBooleanQuery);
                    filterBooleanQuery.addShouldQuery(azzimovNestedQuery);
                }
                azzimovBooleanQueryMajor.addMustQuery(filterBooleanQuery);
            }
            azzimovQueries.addFilterQuery(azzimovBooleanQueryMajor);
        }
        return azzimovQueries;
    }
}
