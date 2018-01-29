package com.azzimov.search.services.search.queries;

import com.azzimov.search.common.dto.LanguageCode;
import com.azzimov.search.common.dto.externals.Attribute;
import com.azzimov.search.common.dto.externals.Category;
import com.azzimov.search.common.dto.externals.Product;
import com.azzimov.search.common.query.AzzimovFilterFunctionQuery;
import com.azzimov.search.common.query.AzzimovFunctionScoreQuery;
import com.azzimov.search.common.query.AzzimovNestedQuery;
import com.azzimov.search.common.query.AzzimovQuery;
import com.azzimov.search.common.query.AzzimovTermTermQuery;
import com.azzimov.search.common.text.AzzimovTextProcessor;
import com.azzimov.search.common.text.AzzimovTextQuery;
import com.azzimov.search.common.util.config.ConfigurationHandler;
import com.azzimov.search.common.util.config.SearchConfiguration;
import com.azzimov.search.services.search.params.AzzimovSearchParameters;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static com.azzimov.search.common.query.AzzimovFilterFunctionQuery.AzzimovScoreFunctionConstant.WEIGHTFACTORFUNCTION;

/**
 * Created by prasad on 1/12/18.
 * AzzimovProductSearchExactQueryCreator provides product search related exact query for ranking
 */
public class AzzimovProductSearchExactQueryCreator extends AzzimovQueryCreator<AzzimovSearchParameters,
        AzzimovQuery, AzzimovFunctionScoreQuery> {
    private ConfigurationHandler configurationHandler;
    // for now, this will be here, in future it should be within the dto libs
    public static final String EXACT_FIELD = "raw_lower";
    public static final String EXACT_FIELD_RAW = "raw";

    public AzzimovProductSearchExactQueryCreator(ConfigurationHandler configurationHandler) {
        this.configurationHandler = configurationHandler;
    }

    @Override
    public AzzimovFunctionScoreQuery createAzzimovQuery(AzzimovSearchParameters azzimovParameters, AzzimovQuery azzimovQuery) {
        List<Object> configList = configurationHandler.getObjectConfigList(SearchConfiguration.QUERY_BOOST_VALUES);
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

        // Retrieve normaized query
        Locale locale = azzimovParameters.getAzzimovSearchRequest()
                .getAzzimovSearchRequestParameters().getLanguage().getLocale();
        Set<AzzimovTextQuery> azzimovTextQueries = new HashSet<>();
        AzzimovTextProcessor azzimovTextProcessor = new AzzimovTextProcessor();
        try {
            azzimovTextQueries  = azzimovTextProcessor.retrieveNGramQueries(query,
                    locale,
                    new ArrayList<>(),
                    2,
                    5);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Map<String, String> targetDocumentTypes = azzimovParameters.getTargetRepositories();
        // Here, for now, we expect one time of targets
        List<String> targetDocs = new ArrayList<>(targetDocumentTypes.keySet());
        // the target repository/index
        String targetRepository = targetDocumentTypes.values().iterator().next();
        // Retrieve the language field for the query language
        LanguageCode languageCode = azzimovParameters.getAzzimovSearchRequest()
                .getAzzimovSearchRequestParameters().getLanguage().getLanguageCode();

        AzzimovFunctionScoreQuery azzimovFunctionScoreQuery = new AzzimovFunctionScoreQuery(targetRepository, targetDocs);
        List<AzzimovFilterFunctionQuery> azzimovFilterFunctionQueryList = new ArrayList<>();
        for (AzzimovTextQuery azzimovTextQuery : azzimovTextQueries) {
            String normalizedQuery = azzimovTextQuery.getProcessedQueryString();
            AzzimovFilterFunctionQuery azzimovFilterFunctionQuery = new AzzimovFilterFunctionQuery();
            AzzimovTermTermQuery azzimovTermTermQuery = new AzzimovTermTermQuery(targetRepository,
                    retrieveFieldPath(Product.PRODUCT_TITLE,
                            LanguageCode.getLanguageField(languageCode),
                            EXACT_FIELD), targetDocs, normalizedQuery);
            azzimovFilterFunctionQuery.setAzzimovQuery(azzimovTermTermQuery);
            azzimovFilterFunctionQuery.setAzzimovScoreFunctionConstant(WEIGHTFACTORFUNCTION);
            azzimovFilterFunctionQuery.setScoreValue(searchFieldBoosts.get(SearchConfiguration.SEARCH_TITLE));
            azzimovFilterFunctionQueryList.add(azzimovFilterFunctionQuery);

            azzimovTermTermQuery = new AzzimovTermTermQuery(targetRepository,
                    retrieveFieldPath(Product.PRODUCT_CATEGORIES,
                            Category.CATEGORY_LABEL,
                            LanguageCode.getLanguageField(languageCode),
                            EXACT_FIELD), targetDocs, normalizedQuery);
            azzimovFilterFunctionQuery = new AzzimovFilterFunctionQuery();
            azzimovFilterFunctionQuery.setAzzimovQuery(azzimovTermTermQuery);
            azzimovFilterFunctionQuery.setAzzimovScoreFunctionConstant(WEIGHTFACTORFUNCTION);
            azzimovFilterFunctionQuery.setScoreValue(searchFieldBoosts.get(SearchConfiguration.SEARCH_CATEGORY_LABEL));
            azzimovFilterFunctionQueryList.add(azzimovFilterFunctionQuery);

            azzimovTermTermQuery = new AzzimovTermTermQuery(targetRepository,
                    retrieveFieldPath(Product.PRODUCT_ATTRIBUTES,
                            Attribute.ATTRIBUTE_STRING_VALUE,
                            LanguageCode.getLanguageField(languageCode),
                            EXACT_FIELD), targetDocs, normalizedQuery);

            AzzimovNestedQuery azzimovNestedQuery = new AzzimovNestedQuery(targetRepository, targetDocs);
            azzimovNestedQuery.setPath(Product.PRODUCT_ATTRIBUTES);
            azzimovNestedQuery.setScoreMode(AzzimovNestedQuery.AzzimovNestedScoreMode.MAX);
            azzimovNestedQuery.setAzzimovQuery(azzimovTermTermQuery);
            azzimovFilterFunctionQuery = new AzzimovFilterFunctionQuery();
            azzimovFilterFunctionQuery.setAzzimovQuery(azzimovNestedQuery);
            azzimovFilterFunctionQuery.setAzzimovScoreFunctionConstant(WEIGHTFACTORFUNCTION);
            azzimovFilterFunctionQuery.setScoreValue(searchFieldBoosts.get(SearchConfiguration.SEARCH_ATTRIBUTE_VALUE));
            azzimovFilterFunctionQueryList.add(azzimovFilterFunctionQuery);

            azzimovTermTermQuery = new AzzimovTermTermQuery(targetRepository,
                    retrieveFieldPath(Product.PRODUCT_ATTRIBUTES,
                            Attribute.ATTRIBUTE_NUMBER_VALUE,
                            LanguageCode.getLanguageField(languageCode),
                            EXACT_FIELD), targetDocs, normalizedQuery);
            azzimovNestedQuery = new AzzimovNestedQuery(targetRepository, targetDocs);
            azzimovNestedQuery.setPath(Product.PRODUCT_ATTRIBUTES);
            azzimovNestedQuery.setScoreMode(AzzimovNestedQuery.AzzimovNestedScoreMode.MAX);
            azzimovNestedQuery.setAzzimovQuery(azzimovTermTermQuery);

            azzimovFilterFunctionQuery = new AzzimovFilterFunctionQuery();
            azzimovFilterFunctionQuery.setAzzimovQuery(azzimovNestedQuery);
            azzimovFilterFunctionQuery.setAzzimovScoreFunctionConstant(WEIGHTFACTORFUNCTION);
            azzimovFilterFunctionQuery.setScoreValue(searchFieldBoosts.get(SearchConfiguration.SEARCH_ATTRIBUTE_VALUE));
            azzimovFilterFunctionQueryList.add(azzimovFilterFunctionQuery);

            azzimovTermTermQuery = new AzzimovTermTermQuery(targetRepository,
                    retrieveFieldPath(Product.PRODUCT_ATTRIBUTES,
                            Attribute.ATTRIBUTE_LABEL,
                            LanguageCode.getLanguageField(languageCode),
                            EXACT_FIELD), targetDocs, normalizedQuery);
            azzimovNestedQuery = new AzzimovNestedQuery(targetRepository, targetDocs);
            azzimovNestedQuery.setPath(Product.PRODUCT_ATTRIBUTES);
            azzimovNestedQuery.setScoreMode(AzzimovNestedQuery.AzzimovNestedScoreMode.MAX);
            azzimovNestedQuery.setAzzimovQuery(azzimovTermTermQuery);

            azzimovFilterFunctionQuery = new AzzimovFilterFunctionQuery();
            azzimovFilterFunctionQuery.setAzzimovQuery(azzimovNestedQuery);
            azzimovFilterFunctionQuery.setAzzimovScoreFunctionConstant(WEIGHTFACTORFUNCTION);
            azzimovFilterFunctionQuery.setScoreValue(searchFieldBoosts.get(SearchConfiguration.SEARCH_ATTRIBUTE_LABEL));
            azzimovFilterFunctionQueryList.add(azzimovFilterFunctionQuery);

            azzimovTermTermQuery = new AzzimovTermTermQuery(targetRepository,
                    retrieveFieldPath(Product.PRODUCT_SHORT_DESCRIPTION,
                            LanguageCode.getLanguageField(languageCode),
                            EXACT_FIELD), targetDocs, normalizedQuery);
            azzimovFilterFunctionQuery = new AzzimovFilterFunctionQuery();
            azzimovFilterFunctionQuery.setAzzimovQuery(azzimovTermTermQuery);
            azzimovFilterFunctionQuery.setAzzimovScoreFunctionConstant(WEIGHTFACTORFUNCTION);
            azzimovFilterFunctionQuery.setScoreValue(searchFieldBoosts.get(SearchConfiguration.SEARCH_SHORT_DESCRIPTION));
            azzimovFilterFunctionQueryList.add(azzimovFilterFunctionQuery);

            azzimovTermTermQuery = new AzzimovTermTermQuery(targetRepository,
                    retrieveFieldPath(Product.PRODUCT_LONG_DESCRIPTION,
                            LanguageCode.getLanguageField(languageCode),
                            EXACT_FIELD), targetDocs, normalizedQuery);
            azzimovFilterFunctionQuery = new AzzimovFilterFunctionQuery();
            azzimovFilterFunctionQuery.setAzzimovQuery(azzimovTermTermQuery);
            azzimovFilterFunctionQuery.setAzzimovScoreFunctionConstant(WEIGHTFACTORFUNCTION);
            azzimovFilterFunctionQuery.setScoreValue(searchFieldBoosts.get(SearchConfiguration.SEARCH_LONG_DESCRIPTION));
            azzimovFilterFunctionQueryList.add(azzimovFilterFunctionQuery);
            azzimovFunctionScoreQuery.setFilterFunctionQueryList(azzimovFilterFunctionQueryList);
            azzimovFunctionScoreQuery.setAzzimovQuery(azzimovQuery);
            azzimovFunctionScoreQuery.setAzzimovScoreModeConstant(AzzimovFunctionScoreQuery.AzzimovScoreModeConstant.MULTIPLY);
            azzimovFunctionScoreQuery.setAzzimovCombineFunctionConstant(
                    AzzimovFunctionScoreQuery.AzzimovCombineFunctionConstant.MULTIPLY);
        }
        azzimovFunctionScoreQuery.setResultOffset(azzimovParameters.getAzzimovSearchRequest()
                .getAzzimovSearchRequestParameters().getResultOffset());
        azzimovFunctionScoreQuery.setResultSize(azzimovParameters.getAzzimovSearchRequest()
                .getAzzimovSearchRequestParameters().getResultsPerPage());
        azzimovFunctionScoreQuery.setQuerySearchType(AzzimovQuery.AzzimovQuerySearchType.DFS_QUERY_THEN_FETCH);
        return azzimovFunctionScoreQuery;
    }
}
