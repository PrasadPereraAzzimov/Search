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
import com.azzimov.search.services.search.learn.SessionCentroidModelCluster;
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
import static com.azzimov.search.services.search.utils.SearchFieldConstants.VALUE_NUM;
import static com.azzimov.search.services.search.utils.SearchFieldConstants.retrieveFieldPath;

/**
 * Created by prasad on 2/1/18.
 * AzzimovProductSearchCentroidSorter extends AzzimovProductSearchSorterCreator that sorts based on different centroid
 * models
 */
public class AzzimovProductSearchCentroidSorter extends AzzimovProductSearchSorterCreator {
    private static final int MAX_N_GRAM_LIMIT = 5;
    private static final int MIN_N_GRAM_LIMIT = 2;
    private static float MIN_SCORE = 0.000001f;

    public AzzimovProductSearchCentroidSorter(ConfigurationHandler configurationHandler) {
        super(configurationHandler);
    }

    @Override
    public List<AzzimovFunctionScoreQuery> createAzzimovQuery(AzzimovSearchParameters azzimovParameters,
                                                              List<AzzimovQuery> azzimovQueryList) {
        AzzimovFunctionScoreQuery azzimovFunctionScoreQuery = null;
        List<AzzimovFunctionScoreQuery> azzimovFunctionScoreQueryList = new ArrayList<>();
        for (LearnCentroidCluster learnCentroidCluster : azzimovParameters.getLearnCentroidClusterList()) {
            LearnCentroidSorterVisitor learnCentroidSorterVisitor = new LearnCentroidSorterVisitor(azzimovParameters,
                    this.getConfigurationHandler(), azzimovQueryList);
            learnCentroidCluster.accept(learnCentroidSorterVisitor);
            azzimovFunctionScoreQuery = learnCentroidSorterVisitor.getAzzimovFunctionScoreQuery();
            azzimovQueryList.clear();
            azzimovQueryList.add(azzimovFunctionScoreQuery);
        }
        azzimovFunctionScoreQueryList.add(azzimovFunctionScoreQuery);
        return azzimovFunctionScoreQueryList;
    }

    public static class LearnCentroidSorterVisitor implements LearnCentroidCluster.LearnCentroidClusterVisitor {
        private AzzimovSearchParameters azzimovSearchParameters;
        private ConfigurationHandler configurationHandler;
        private List<AzzimovQuery> azzimovQueryList;
        private AzzimovFunctionScoreQuery azzimovFunctionScoreQuery;

        public LearnCentroidSorterVisitor(AzzimovSearchParameters azzimovSearchParameters,
                                          ConfigurationHandler configurationHandler,
                                          List<AzzimovQuery> azzimovQueryList) {
            this.azzimovSearchParameters = azzimovSearchParameters;
            this.configurationHandler = configurationHandler;
            this.azzimovQueryList = azzimovQueryList;
        }

        @Override
        public void visit(LearnCentroidCluster learnCentroidCluster) {
            Map<String, String> targetDocumentTypes = azzimovSearchParameters.getTargetRepositories();
            // Here, for now, we expect one time of targets
            List<String> targetDocs = new ArrayList<>();
            targetDocs.add(Product.PRODUCT_EXTERNAL_NAME);
            // the target repository/index
            String targetRepository = targetDocumentTypes.get(Product.PRODUCT_EXTERNAL_NAME);
            // Retrieve the language field for the query language
            LanguageCode languageCode = azzimovSearchParameters.getAzzimovSearchRequest()
                    .getAzzimovSearchRequestParameters().getLanguage().getLanguageCode();

            // Retrieve normaized query
            Locale locale = azzimovSearchParameters.getAzzimovSearchRequest()
                    .getAzzimovSearchRequestParameters().getLanguage().getLocale();
            Set<AzzimovTextQuery> azzimovTextQueries = new HashSet<>();
            AzzimovTextProcessor azzimovTextProcessor = new AzzimovTextProcessor();
            String query = azzimovSearchParameters.getAzzimovSearchRequest().
                    getAzzimovSearchRequestParameters().getQuery();
            AzzimovQuery azzimovInputQuery = azzimovQueryList.get(0);
            this.azzimovFunctionScoreQuery = null;

            Map<String, Map<FeedbackAttribute, Float>> feedbackAttributeCentroids = learnCentroidCluster.getAttributeCentroids();
            Map<FeedbackAttribute, Float> feedbackAttributeFloatMap = new HashMap<>();
            try {
                azzimovTextQueries = azzimovTextProcessor.retrieveNGramTokenizedQueries(query,
                        locale,
                        new ArrayList<>(),
                        MIN_N_GRAM_LIMIT,
                        MAX_N_GRAM_LIMIT);
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
            }
            AzzimovFilterFunctionQuery azzimovFilterFunctionQuery = new AzzimovFilterFunctionQuery();
            azzimovFilterFunctionQuery.setAzzimovQuery(new AzzimovMatchAllQuery(targetRepository, targetDocs));
            azzimovFilterFunctionQuery.setAzzimovScoreFunctionConstant(WEIGHTFACTORFUNCTION);
            azzimovFilterFunctionQuery.setScoreValue(MIN_SCORE);
            azzimovFilterFunctionQueryList.add(azzimovFilterFunctionQuery);

            azzimovFunctionScoreQuery.setAzzimovScoreModeConstant(AzzimovFunctionScoreQuery.AzzimovScoreModeConstant.SUM);
            azzimovFunctionScoreQuery.setAzzimovCombineFunctionConstant(
                    AzzimovFunctionScoreQuery.AzzimovCombineFunctionConstant.SUM);
            azzimovFunctionScoreQuery.setAzzimovQuery(azzimovInputQuery);
            azzimovFunctionScoreQuery.setFilterFunctionQueryList(azzimovFilterFunctionQueryList);
            azzimovFunctionScoreQuery.setResultOffset(azzimovSearchParameters.getAzzimovSearchRequest()
                    .getAzzimovSearchRequestParameters().getResultOffset());
            azzimovFunctionScoreQuery.setResultSize(azzimovSearchParameters.getAzzimovSearchRequest()
                    .getAzzimovSearchRequestParameters().getResultsPerPage());
            azzimovFunctionScoreQuery.setQuerySearchType(AzzimovQuery.AzzimovQuerySearchType.DFS_QUERY_THEN_FETCH);
            // Create score query that normalize the query score for proper relevance we need in query equation
            AzzimovProductSearchScoreQueryCreator azzimovProductSearchScoreQueryCreator =
                    new AzzimovProductSearchScoreQueryCreator(this.configurationHandler);
            azzimovFunctionScoreQuery = azzimovProductSearchScoreQueryCreator
                    .createAzzimovQuery(azzimovSearchParameters, azzimovFunctionScoreQuery);
            this.azzimovFunctionScoreQuery = azzimovFunctionScoreQuery;
        }

        public AzzimovFunctionScoreQuery getAzzimovFunctionScoreQuery() {
            return azzimovFunctionScoreQuery;
        }

        @Override
        public void visit(SessionCentroidModelCluster sessionCentroidModelCluster) {
            Map<String, String> targetDocumentTypes = azzimovSearchParameters.getTargetRepositories();
            // Here, for now, we expect one time of targets
            List<String> targetDocs = new ArrayList<>();
            targetDocs.add(Product.PRODUCT_EXTERNAL_NAME);
            // the target repository/index
            String targetRepository = targetDocumentTypes.get(Product.PRODUCT_EXTERNAL_NAME);
            // Retrieve the language field for the query language
            LanguageCode languageCode = azzimovSearchParameters.getAzzimovSearchRequest()
                    .getAzzimovSearchRequestParameters().getLanguage().getLanguageCode();

            // Retrieve normaized query
            Locale locale = azzimovSearchParameters.getAzzimovSearchRequest()
                    .getAzzimovSearchRequestParameters().getLanguage().getLocale();
            Set<AzzimovTextQuery> azzimovTextQueries = new HashSet<>();
            AzzimovTextProcessor azzimovTextProcessor = new AzzimovTextProcessor();
            String query = azzimovSearchParameters.getAzzimovSearchRequest().
                    getAzzimovSearchRequestParameters().getQuery();
            AzzimovQuery azzimovInputQuery = azzimovQueryList.get(0);
            this.azzimovFunctionScoreQuery = null;
            try {
                azzimovTextQueries = azzimovTextProcessor.retrieveNGramTokenizedQueries(query,
                        locale,
                        new ArrayList<>(),
                        MIN_N_GRAM_LIMIT,
                        MAX_N_GRAM_LIMIT);
            } catch (IOException e) {
                e.printStackTrace();
            }
            Map<String, List<SessionCentroidModelCluster.SessionCentroid>> attributeCentroidsMap  =
                    sessionCentroidModelCluster.getSessionLearningModelClusterMap();
            List<AzzimovFilterFunctionQuery> azzimovFilterFunctionQueryList = new ArrayList<>();
            List<SessionCentroidModelCluster.SessionCentroid> sessionCentroidList = new ArrayList<>();
            for (AzzimovTextQuery azzimovTextQuery : azzimovTextQueries) {
                if (attributeCentroidsMap.containsKey(azzimovTextQuery.getProcessedQueryString()))
                    sessionCentroidList.addAll(
                            attributeCentroidsMap.get(azzimovTextQuery.getProcessedQueryString()));
            }
            AzzimovFunctionScoreQuery azzimovFunctionScoreQuery = new AzzimovFunctionScoreQuery(targetRepository, targetDocs);
            for (SessionCentroidModelCluster.SessionCentroid sessionCentroid : sessionCentroidList) {
                for (Map.Entry<String, SessionCentroidModelCluster.SessionCentroidEntry> sessionCentroidEntryEntry:
                     sessionCentroid.getSessionLearningEntryMap().entrySet()) {
                    SessionCentroidModelCluster.SessionCentroidEntry sessionCentroidEntry =
                            sessionCentroidEntryEntry.getValue();
                    FeedbackAttribute feedbackAttribute = sessionCentroidEntry.getFeedbackAttribute();
                    float weight = sessionCentroidEntry.getCount();

                    AzzimovBooleanQuery attributeBooleanQuery = new AzzimovBooleanQuery(targetRepository, targetDocs);
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
                    azzimovFilterFunctionQuery.setScoreValue(weight);
                    azzimovFilterFunctionQuery.setAzzimovQuery(azzimovNestedQuery);
                    azzimovFilterFunctionQueryList.add(azzimovFilterFunctionQuery);
                }
            }
            AzzimovFilterFunctionQuery azzimovFilterFunctionQuery = new AzzimovFilterFunctionQuery();
            azzimovFilterFunctionQuery.setAzzimovQuery(new AzzimovMatchAllQuery(targetRepository, targetDocs));
            azzimovFilterFunctionQuery.setAzzimovScoreFunctionConstant(WEIGHTFACTORFUNCTION);
            azzimovFilterFunctionQuery.setScoreValue(MIN_SCORE);
            azzimovFilterFunctionQueryList.add(azzimovFilterFunctionQuery);
            azzimovFunctionScoreQuery.setAzzimovScoreModeConstant(AzzimovFunctionScoreQuery.AzzimovScoreModeConstant.SUM);
            azzimovFunctionScoreQuery.setAzzimovCombineFunctionConstant(
                    AzzimovFunctionScoreQuery.AzzimovCombineFunctionConstant.SUM);
            azzimovFunctionScoreQuery.setAzzimovQuery(azzimovInputQuery);
            azzimovFunctionScoreQuery.setFilterFunctionQueryList(azzimovFilterFunctionQueryList);
            azzimovFunctionScoreQuery.setResultOffset(azzimovSearchParameters.getAzzimovSearchRequest()
                    .getAzzimovSearchRequestParameters().getResultOffset());
            azzimovFunctionScoreQuery.setResultSize(azzimovSearchParameters.getAzzimovSearchRequest()
                    .getAzzimovSearchRequestParameters().getResultsPerPage());
            azzimovFunctionScoreQuery.setQuerySearchType(AzzimovQuery.AzzimovQuerySearchType.DFS_QUERY_THEN_FETCH);

            // Create score query that normalize the query score for proper relevance we need in query equation
            AzzimovProductSearchScoreQueryCreator azzimovProductSearchScoreQueryCreator =
                    new AzzimovProductSearchScoreQueryCreator(this.configurationHandler);
            azzimovFunctionScoreQuery = azzimovProductSearchScoreQueryCreator
                    .createAzzimovQuery(azzimovSearchParameters, azzimovFunctionScoreQuery);
            this.azzimovFunctionScoreQuery = azzimovFunctionScoreQuery;
        }
    }
}
