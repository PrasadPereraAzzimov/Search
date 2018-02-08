package com.azzimov.search.services.search.sorters.product;

import com.azzimov.search.common.dto.LanguageCode;
import com.azzimov.search.common.dto.externals.Attribute;
import com.azzimov.search.common.dto.externals.Product;
import com.azzimov.search.common.dto.internals.feedback.FeedbackAttribute;
import com.azzimov.search.common.query.AzzimovBooleanQuery;
import com.azzimov.search.common.query.AzzimovFilterFunctionQuery;
import com.azzimov.search.common.query.AzzimovFunctionScoreQuery;
import com.azzimov.search.common.query.AzzimovMatchAllQuery;
import com.azzimov.search.common.query.AzzimovNestedQuery;
import com.azzimov.search.common.query.AzzimovQuery;
import com.azzimov.search.common.query.AzzimovTermTermQuery;
import com.azzimov.search.common.text.AzzimovTextProcessor;
import com.azzimov.search.common.text.AzzimovTextQuery;
import com.azzimov.search.common.util.config.ConfigurationHandler;
import com.azzimov.search.services.search.learn.LearnCentroidCluster;
import com.azzimov.search.services.search.params.product.AzzimovSearchParameters;
import com.azzimov.search.services.search.queries.product.AzzimovProductSearchScoreQueryCreator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static com.azzimov.search.common.query.AzzimovFilterFunctionQuery.AzzimovScoreFunctionConstant.WEIGHTFACTORFUNCTION;
import static com.azzimov.search.services.search.utils.SearchFieldConstants.EXACT_FIELD_RAW;
import static com.azzimov.search.services.search.utils.SearchFieldConstants.VALUE_DATE;
import static com.azzimov.search.services.search.utils.SearchFieldConstants.VALUE_NUM;
import static com.azzimov.search.services.search.utils.SearchFieldConstants.retrieveFieldPath;

/**
 * Created by prasad on 2/1/18.
 */
public class AzzimovProductSearchCentroidSorter extends AzzimovProductSearchSorterCreator {
    public AzzimovProductSearchCentroidSorter(ConfigurationHandler configurationHandler,
                                              List<LearnCentroidCluster> learnCentroidClusterList) {
        super(configurationHandler, learnCentroidClusterList);
    }

    @Override
    public List<AzzimovFunctionScoreQuery> createAzzimovQuery(AzzimovSearchParameters azzimovParameters,
                                                              List<AzzimovQuery> azzimovQueryList) {
        Map<String, String> targetDocumentTypes = azzimovParameters.getTargetRepositories();
        // Here, for now, we expect one time of targets
        List<String> targetDocs = new ArrayList<>();
        targetDocs.add(Product.PRODUCT_EXTERNAL_NAME);
        // the target repository/index
        String targetRepository = targetDocumentTypes.get(Product.PRODUCT_EXTERNAL_NAME);
        // Retrieve the language field for the query language
        LanguageCode languageCode = azzimovParameters.getAzzimovSearchRequest()
                .getAzzimovSearchRequestParameters().getLanguage().getLanguageCode();

        // Retrieve normaized query
        Locale locale = azzimovParameters.getAzzimovSearchRequest()
                .getAzzimovSearchRequestParameters().getLanguage().getLocale();
        Set<AzzimovTextQuery> azzimovTextQueries = new HashSet<>();
        AzzimovTextProcessor azzimovTextProcessor = new AzzimovTextProcessor();
        String query = azzimovParameters.getAzzimovSearchRequest().
                getAzzimovSearchRequestParameters().getQuery();
        List<AzzimovFunctionScoreQuery> azzimovFunctionScoreQueryList = new ArrayList<>();

        AzzimovQuery azzimovInputQuery = azzimovQueryList.get(0);
        AzzimovFunctionScoreQuery azzimovOutputQuery = null;

        for (LearnCentroidCluster learnCentroidCluster : getLearnStatModelServices()) {
            Map<String, Map<FeedbackAttribute, Float>> feedbackAttributeCentroids = learnCentroidCluster.getAttributeCentroids();
            Map<FeedbackAttribute, Float> feedbackAttributeFloatMap = new HashMap<>();
            try {
                azzimovTextQueries = azzimovTextProcessor.retrieveNGramQueries(query,
                        locale,
                        new ArrayList<>(),
                        2,
                        5);
            } catch (IOException e) {
                e.printStackTrace();
            }
            for (AzzimovTextQuery azzimovTextQuery : azzimovTextQueries) {
                if (feedbackAttributeCentroids.containsKey(azzimovTextQuery.getProcessedQueryString()))
                    feedbackAttributeFloatMap.putAll(
                            feedbackAttributeCentroids.get(azzimovTextQuery.getProcessedQueryString()));
            }
            AzzimovFunctionScoreQuery azzimovFunctionScoreQuery = new AzzimovFunctionScoreQuery(targetRepository, targetDocs);
            List<AzzimovFilterFunctionQuery> azzimovFilterFunctionQueryList = new ArrayList<>();
            for (Map.Entry<FeedbackAttribute, Float> entry : feedbackAttributeFloatMap.entrySet()) {
                AzzimovBooleanQuery attributeBooleanQuery = new AzzimovBooleanQuery(targetRepository, targetDocs);
                FeedbackAttribute feedbackAttribute = entry.getKey();
                AzzimovTermTermQuery azzimovTermTermQuery = new AzzimovTermTermQuery(targetRepository,
                        retrieveFieldPath(Product.PRODUCT_ATTRIBUTES,
                                Attribute.ATTRIBUTE_LABEL,
                                LanguageCode.getLanguageField(languageCode),
                                EXACT_FIELD_RAW), targetDocs, feedbackAttribute.getFeedbackAttributeLabel().getLabel());
                attributeBooleanQuery.addMustQuery(azzimovTermTermQuery);
                if (feedbackAttribute.getFeedbackAttributeStringValue() != null &&
                        !feedbackAttribute.getFeedbackAttributeStringValue().getStrValue().isEmpty()) {
                    azzimovTermTermQuery = new AzzimovTermTermQuery(targetRepository,
                            retrieveFieldPath(Product.PRODUCT_ATTRIBUTES,
                                    Attribute.ATTRIBUTE_STRING_VALUE,
                                    LanguageCode.getLanguageField(languageCode),
                                    EXACT_FIELD_RAW), targetDocs,
                            feedbackAttribute.getFeedbackAttributeStringValue().getStrValue());
                    attributeBooleanQuery.addMustQuery(azzimovTermTermQuery);
                } else {
                    Double queryValue = feedbackAttribute.getFeedbackAttributeNumericValue().getNumericValue();
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
                AzzimovFilterFunctionQuery azzimovFilterFunctionQuery = new AzzimovFilterFunctionQuery();
                azzimovFilterFunctionQuery.setAzzimovScoreFunctionConstant(WEIGHTFACTORFUNCTION);
                azzimovFilterFunctionQuery.setScoreValue(entry.getValue());
                azzimovFilterFunctionQuery.setAzzimovQuery(azzimovNestedQuery);
                azzimovFilterFunctionQueryList.add(azzimovFilterFunctionQuery);
                System.out.println("---> Adding for attributes = " + feedbackAttribute.getFeedbackAttributeLabel().getLabel());
            }
            AzzimovFilterFunctionQuery azzimovFilterFunctionQuery = new AzzimovFilterFunctionQuery();
            azzimovFilterFunctionQuery.setAzzimovQuery(new AzzimovMatchAllQuery(targetRepository, targetDocs));
            azzimovFilterFunctionQuery.setAzzimovScoreFunctionConstant(WEIGHTFACTORFUNCTION);
            azzimovFilterFunctionQuery.setScoreValue(0.000001f);
            azzimovFilterFunctionQueryList.add(azzimovFilterFunctionQuery);

            azzimovFunctionScoreQuery.setAzzimovScoreModeConstant(AzzimovFunctionScoreQuery.AzzimovScoreModeConstant.SUM);
            azzimovFunctionScoreQuery.setAzzimovCombineFunctionConstant(
                    AzzimovFunctionScoreQuery.AzzimovCombineFunctionConstant.SUM);
            azzimovFunctionScoreQuery.setAzzimovQuery(azzimovInputQuery);
            azzimovFunctionScoreQuery.setFilterFunctionQueryList(azzimovFilterFunctionQueryList);
            azzimovFunctionScoreQuery.setResultOffset(azzimovParameters.getAzzimovSearchRequest()
                    .getAzzimovSearchRequestParameters().getResultOffset());
            azzimovFunctionScoreQuery.setResultSize(azzimovParameters.getAzzimovSearchRequest()
                    .getAzzimovSearchRequestParameters().getResultsPerPage());
            azzimovFunctionScoreQuery.setQuerySearchType(AzzimovQuery.AzzimovQuerySearchType.DFS_QUERY_THEN_FETCH);

            // Create score query that normalize the query score for proper relevance we need in query equation
            AzzimovProductSearchScoreQueryCreator azzimovProductSearchScoreQueryCreator =
                    new AzzimovProductSearchScoreQueryCreator(getConfigurationHandler());
            azzimovFunctionScoreQuery = azzimovProductSearchScoreQueryCreator
                    .createAzzimovQuery(azzimovParameters, azzimovFunctionScoreQuery);

            azzimovInputQuery = azzimovFunctionScoreQuery;
            azzimovOutputQuery = azzimovFunctionScoreQuery;
        }
        if (azzimovOutputQuery != null)
            azzimovFunctionScoreQueryList.add(azzimovOutputQuery);
        return azzimovFunctionScoreQueryList;
    }
}
