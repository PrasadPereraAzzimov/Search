package com.azzimov.search.services.feedback;

import com.azzimov.search.common.dto.Language;
import com.azzimov.search.common.dto.communications.requests.AzzimovRequestFilter;
import com.azzimov.search.common.dto.communications.requests.search.AzzimovSearchRequest;
import com.azzimov.search.common.dto.communications.responses.search.AzzimovSearchResponse;
import com.azzimov.search.common.dto.externals.AzzimovRequestRefinement;
import com.azzimov.search.common.dto.internals.feedback.Feedback;
import com.azzimov.search.common.dto.internals.feedback.FeedbackAttribute;
import com.azzimov.search.common.dto.internals.feedback.FeedbackAttributeLabel;
import com.azzimov.search.common.dto.internals.feedback.FeedbackAttributeNumericValue;
import com.azzimov.search.common.dto.internals.feedback.FeedbackAttributeStringValue;
import com.azzimov.search.common.dto.internals.feedback.FeedbackCategory;
import com.azzimov.search.common.dto.internals.feedback.FeedbackCategoryLabel;
import com.azzimov.search.common.dto.internals.feedback.FeedbackType;
import com.azzimov.search.common.dto.internals.feedback.GuidanceAttributeEntry;
import com.azzimov.search.common.dto.internals.feedback.GuidanceCategoryEntry;
import com.azzimov.search.common.dto.internals.feedback.GuidanceFeedback;
import com.azzimov.search.common.dto.internals.feedback.ProductFeedback;
import com.azzimov.search.common.dto.internals.feedback.ProductResult;
import com.azzimov.search.common.dto.internals.feedback.QueryFeedback;
import com.azzimov.search.common.dto.internals.feedback.QuerySearchResult;
import com.azzimov.search.common.dto.internals.feedback.visitor.FeedbackVisitor;
import com.azzimov.search.common.query.AzzimovMatchAllQuery;
import com.azzimov.search.common.requests.AzzimovIndexPersistRequest;
import com.azzimov.search.common.responses.AzzimovIndexResponse;
import com.azzimov.search.common.responses.AzzimovSourceResponse;
import com.azzimov.search.common.util.config.ConfigurationHandler;
import com.azzimov.search.common.util.config.SearchConfiguration;
import com.azzimov.search.listeners.ConfigListener;
import com.azzimov.search.services.search.executors.SearchExecutorService;
import com.azzimov.search.services.search.executors.feedback.AzzimovFeedbackQueryExecutor;
import com.azzimov.search.services.search.params.product.AzzimovSearchParameters;
import com.azzimov.search.services.search.validators.product.AzzimovSearchRequestValidator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.azzimov.search.services.search.utils.SearchFieldConstants.VALUE_TEXT;

/**
 * Created by prasad on 1/19/18.
 * Feedback persist manager is the application component that responsible of building proper feedback documents and
 * persisting them in feedback repositories
 */
public class FeedbackPersistManager {
    private static final Logger logger = LogManager.getLogger(FeedbackPersistManager.class);
    private SearchExecutorService searchExecutorService;
    private ConfigurationHandler configurationHandler;

    public FeedbackPersistManager(SearchExecutorService searchExecutorService,
                                  ConfigurationHandler configurationHandler) {
        this.searchExecutorService = searchExecutorService;
        this.configurationHandler = configurationHandler;
    }

    public boolean persistFeedback(List<? extends Feedback> feedbackList, AzzimovSearchRequest azzimovSearchRequest) throws Exception {
        FeedbackPersistVisitor feedbackPersistVisitor = new FeedbackPersistVisitor(searchExecutorService,
                configurationHandler, azzimovSearchRequest);
        boolean succeed = true;
        for (Feedback feedback : feedbackList) {
            succeed = succeed && feedback.accept(feedbackPersistVisitor);
        }
        return succeed;
    }

    public Feedback persistFeedback(AzzimovSearchRequest azzimovSearchRequest,
                                   AzzimovSearchResponse azzimovSearchResponse) throws Exception {
        Set<ProductResult> productResultList = new HashSet<>();
        int index = azzimovSearchRequest.getAzzimovSearchRequestParameters().getResultOffset();
        for (Long productId : azzimovSearchResponse.getAzzimovSearchResponseParameter().getProductIds()) {
            productResultList.add(new ProductResult(productId, index++, null));
        }
        QuerySearchResult querySearchResult = new QuerySearchResult(
                azzimovSearchRequest.getAzzimovSearchRequestParameters().getResultOffset(),
                azzimovSearchRequest.getAzzimovSearchRequestParameters().getResultsPerPage(),
                azzimovSearchResponse.getAzzimovSearchInfo().getCount(),
                productResultList,
                null);
        Feedback feedback;
        if ((azzimovSearchRequest.getAzzimovSearchRequestParameters().getAzzimovRequestFilters() != null &&
                !azzimovSearchRequest.getAzzimovSearchRequestParameters().getAzzimovRequestFilters().isEmpty())
                ||
                (azzimovSearchRequest.getAzzimovSearchRequestParameters().getAzzimovRequestRefinement() != null)) {
            List<AzzimovRequestFilter> azzimovRequestFilterList = azzimovSearchRequest.
                    getAzzimovSearchRequestParameters().getAzzimovRequestFilters();
            List<GuidanceAttributeEntry> guidanceAttributeEntryList = new ArrayList<>();
            if (azzimovRequestFilterList != null && !azzimovRequestFilterList.isEmpty()) {
                for (AzzimovRequestFilter azzimovRequestFilter : azzimovRequestFilterList) {
                    FeedbackAttributeLabel attributeLabel = new FeedbackAttributeLabel(azzimovRequestFilter.getLabel());
                    FeedbackAttribute feedbackAttribute = new FeedbackAttribute(attributeLabel, 0);
                    if (azzimovRequestFilter.getFilterType().equals(VALUE_TEXT)) {
                        FeedbackAttributeStringValue feedbackAttributeStringValue =
                                new FeedbackAttributeStringValue(azzimovRequestFilter.getValue());
                        feedbackAttribute.setFeedbackAttributeStringValue(feedbackAttributeStringValue);
                    } else {
                        FeedbackAttributeNumericValue feedbackAttributeNumericValue =
                                new FeedbackAttributeNumericValue(Double.parseDouble(azzimovRequestFilter.getValue()));
                        feedbackAttribute.setFeedbackAttributeNumericValue(feedbackAttributeNumericValue);
                        feedbackAttribute.setUnit(azzimovRequestFilter.getFilterType());
                    }
                    GuidanceAttributeEntry guidanceAttributeEntry = new GuidanceAttributeEntry(feedbackAttribute, 0);
                    guidanceAttributeEntryList.add(guidanceAttributeEntry);
                }
            }
            List<GuidanceCategoryEntry> guidanceCategoryEntryList = new ArrayList<>();
            AzzimovRequestRefinement azzimovRequestRefinement = azzimovSearchRequest.
                    getAzzimovSearchRequestParameters().getAzzimovRequestRefinement();
            if (azzimovRequestRefinement != null) {
                FeedbackCategoryLabel feedbackCategoryValue = new FeedbackCategoryLabel(azzimovRequestRefinement.getValue());
                FeedbackCategory feedbackCategory = new FeedbackCategory(feedbackCategoryValue, 0, 0);
                GuidanceCategoryEntry guidanceCategoryEntry = new GuidanceCategoryEntry(feedbackCategory, 0);
                guidanceCategoryEntryList.add(guidanceCategoryEntry);
            }
            Feedback.FeedbackBuilder feedbackBuilder = new Feedback.FeedbackBuilder(
                    FeedbackType.GUIDANCE)
                    .setDevice(azzimovSearchRequest.getAzzimovUserRequestParameters().getBrowserDevice())
                    .setInteractionTime(DateTime.now(DateTimeZone.UTC))
                    .setIp(azzimovSearchRequest.getAzzimovUserRequestParameters().getMemberIp())
                    .setLanguage(new Language(azzimovSearchRequest.getAzzimovSearchRequestParameters()
                            .getLanguage().getLanguageCode()))
                    .setMemberId(azzimovSearchRequest.getAzzimovUserRequestParameters().getMemberId())
                    .setQuery(azzimovSearchRequest.getAzzimovSearchRequestParameters().getQuery())
                    .setRequestId(azzimovSearchRequest.getAzzimovUserRequestParameters().getRequestId())
                    .setSessionId(azzimovSearchRequest.getAzzimovUserRequestParameters().getSessionId())
                    .setQueryHitCount(azzimovSearchResponse.getAzzimovSearchInfo().getCount())
                    .setGuidanceAttributeEntry(guidanceAttributeEntryList)
                    .setGuidanceCategoryEntry(guidanceCategoryEntryList)
                    .setQuerySearchResult(querySearchResult);
            feedback = feedbackBuilder.build();
        } else {
            Feedback.FeedbackBuilder feedbackBuilder = new Feedback.FeedbackBuilder(
                    FeedbackType.QUERY)
                    .setDevice(azzimovSearchRequest.getAzzimovUserRequestParameters().getBrowserDevice())
                    .setInteractionTime(DateTime.now(DateTimeZone.UTC))
                    .setIp(azzimovSearchRequest.getAzzimovUserRequestParameters().getMemberIp())
                    .setLanguage(new Language(azzimovSearchRequest.getAzzimovSearchRequestParameters()
                            .getLanguage().getLanguageCode()))
                    .setMemberId(azzimovSearchRequest.getAzzimovUserRequestParameters().getMemberId())
                    .setQuery(azzimovSearchRequest.getAzzimovSearchRequestParameters().getQuery())
                    .setRequestId(azzimovSearchRequest.getAzzimovUserRequestParameters().getRequestId())
                    .setSessionId(azzimovSearchRequest.getAzzimovUserRequestParameters().getSessionId())
                    .setQueryHitCount(azzimovSearchResponse.getAzzimovSearchInfo().getCount())
                    .setQuerySearchResult(querySearchResult);
            feedback = feedbackBuilder.build();
        }
        List<Feedback> feedbackList = new ArrayList<>();
        feedbackList.add(feedback);
        persistFeedback(feedbackList, azzimovSearchRequest);
        return feedback;
    }

    private static class FeedbackPersistVisitor implements FeedbackVisitor<Boolean> {
        private SearchExecutorService searchExecutorService;
        private ConfigurationHandler configurationHandler;
        private AzzimovSearchRequest azzimovSearchRequest;

        FeedbackPersistVisitor(SearchExecutorService searchExecutorService,
                               ConfigurationHandler configurationHandler,
                               AzzimovSearchRequest azzimovSearchRequest) {
            this.searchExecutorService = searchExecutorService;
            this.configurationHandler = configurationHandler;
            this.azzimovSearchRequest = azzimovSearchRequest;
        }

        @Override
        public Boolean visit(QueryFeedback queryFeedback) throws Exception {
            AzzimovIndexPersistRequest<QueryFeedback> azzimovIndexPersistRequest =
                    new AzzimovIndexPersistRequest<>();
            Map<String, QueryFeedback> productFeedbackMap = new HashMap<>();
            productFeedbackMap.put("" + DateTime.now(DateTimeZone.UTC), queryFeedback);
            azzimovIndexPersistRequest.setSource(productFeedbackMap);
            List<String> documentTypes = new ArrayList<>();
            documentTypes.add(FeedbackType.QUERY.toString());
            List<Object> targetRepositoriesConfigs = configurationHandler
                    .getObjectConfigList(SearchConfiguration.FEEDBACK_DOCUMENT_TARGETS);
            Map<String, String> targetRepositories = ConfigListener.retrieveTargetRepositoriesforDocuments(
                    targetRepositoriesConfigs,
                    configurationHandler);
            // the target repository/index
            String targetRepository = targetRepositories.values().iterator().next();
            azzimovIndexPersistRequest.setAzzimovQuery(new AzzimovMatchAllQuery(targetRepository, documentTypes));
            AzzimovSourceResponse<AzzimovIndexResponse> azzimovSourceResponse =
                    searchExecutorService.getExecutorService().performPersistRequest(azzimovIndexPersistRequest);
            logger.info("Persisted document = {}", azzimovSourceResponse.getObjectType().get(0).getStatus());
            return true;
        }

        @Override
        public Boolean visit(ProductFeedback productFeedback) throws Exception {
            AzzimovIndexPersistRequest<ProductFeedback> azzimovIndexPersistRequest =
                    new AzzimovIndexPersistRequest<>();
            Map<String, ProductFeedback> productFeedbackMap = new HashMap<>();
            productFeedback.setInteractionTime(DateTime.now(DateTimeZone.UTC));
            productFeedbackMap.put("" + productFeedback.getOriginalProductId() + DateTime.now(), productFeedback);
            azzimovIndexPersistRequest.setSource(productFeedbackMap);
            List<String> documentTypes = new ArrayList<>();
            documentTypes.add(FeedbackType.PRODUCT.toString());
            List<Object> targetRepositoriesConfigs = configurationHandler
                    .getObjectConfigList(SearchConfiguration.FEEDBACK_DOCUMENT_TARGETS);
            Map<String, String> targetRepositories = ConfigListener.retrieveTargetRepositoriesforDocuments(targetRepositoriesConfigs,
                    configurationHandler);
            // the target repository/index
            String targetRepository = targetRepositories.values().iterator().next();
            azzimovIndexPersistRequest.setAzzimovQuery(new AzzimovMatchAllQuery(targetRepository, documentTypes));
            AzzimovSourceResponse<AzzimovIndexResponse> azzimovSourceResponse =
                    searchExecutorService.getExecutorService().performPersistRequest(azzimovIndexPersistRequest);
            logger.info("Persisted document = {}", azzimovSourceResponse.getObjectType().get(0).getStatus());
            return true;
        }

        @Override
        public Boolean visit(GuidanceFeedback guidanceFeedback) throws Exception {
            AzzimovIndexPersistRequest<GuidanceFeedback> azzimovIndexPersistRequest =
                    new AzzimovIndexPersistRequest<>();
            Map<String, GuidanceFeedback> productFeedbackMap = new HashMap<>();
            productFeedbackMap.put("" + DateTime.now(DateTimeZone.UTC), guidanceFeedback);
            azzimovIndexPersistRequest.setSource(productFeedbackMap);
            List<String> documentTypes = new ArrayList<>();
            documentTypes.add(FeedbackType.GUIDANCE.toString());
            List<Object> targetRepositoriesConfigs = configurationHandler
                    .getObjectConfigList(SearchConfiguration.FEEDBACK_DOCUMENT_TARGETS);
            Map<String, String> targetRepositories = ConfigListener.retrieveTargetRepositoriesforDocuments(
                    targetRepositoriesConfigs,
                    configurationHandler);
            // the target repository/index
            findGuidanceResultCounts(guidanceFeedback, azzimovSearchRequest, searchExecutorService, configurationHandler);
            String targetRepository = targetRepositories.values().iterator().next();
            azzimovIndexPersistRequest.setAzzimovQuery(new AzzimovMatchAllQuery(targetRepository, documentTypes));
            AzzimovSourceResponse<AzzimovIndexResponse> azzimovSourceResponse =
                    searchExecutorService.getExecutorService().performPersistRequest(azzimovIndexPersistRequest);
            logger.info("Persisted document = {}", azzimovSourceResponse.getObjectType().get(0).getStatus());
            return true;

        }

        /**
         * This function query the product repository and find each guidance/refinement related results counts before
         * persisting them
         * @param guidanceFeedback the created guidance feedback
         */
        private void findGuidanceResultCounts(GuidanceFeedback guidanceFeedback,
                                              AzzimovSearchRequest azzimovSearchRequest,
                                              SearchExecutorService searchExecutorService,
                                              ConfigurationHandler configurationHandler)
                throws IllegalAccessException, IOException, InstantiationException {
            AzzimovFeedbackQueryExecutor azzimovFeedbackQueryExecutor = new AzzimovFeedbackQueryExecutor(configurationHandler,
                    searchExecutorService);
            AzzimovSearchRequestValidator azzimovSearchRequestValidator = new AzzimovSearchRequestValidator(configurationHandler);
            AzzimovSearchParameters azzimovSearchParameters = azzimovSearchRequestValidator
                    .validateRequest(azzimovSearchRequest);
            List<AzzimovSearchParameters> azzimovSearchParametersList = new ArrayList<>();
            azzimovSearchParametersList.add(azzimovSearchParameters);
            List<AzzimovSearchResponse> azzimovSearchResponseList =
                    azzimovFeedbackQueryExecutor.search(azzimovSearchParametersList);
            // Based on order we go through in the AzzimovFeedbackQueryExecutor, we add the results count to guidance
            // feedbacks
            int results = 20;
            Iterator<AzzimovSearchResponse> azzimovSearchResponseIterator = azzimovSearchResponseList.iterator();
            AzzimovSearchResponse azzimovSearchResponse = azzimovSearchResponseIterator.next();
            //guidanceFeedback.setQueryHitCount(azzimovSearchResponse.getAzzimovSearchInfo().getCount());
            guidanceFeedback.setQueryHitCount(results);
            for (GuidanceAttributeEntry guidanceAttributeEntry : guidanceFeedback.getGuidanceAttributeEntries()) {
                azzimovSearchResponse = azzimovSearchResponseIterator.next();
                guidanceAttributeEntry.setResultCount(azzimovSearchResponse.getAzzimovSearchInfo().getCount());
                //guidanceAttributeEntry.setResultCount(results - 5);
            }
            for (GuidanceCategoryEntry guidanceCategoryEntry : guidanceFeedback.getGuidanceCategoryEntries()) {
                azzimovSearchResponse = azzimovSearchResponseIterator.next();
                guidanceCategoryEntry.setResultCount(azzimovSearchResponse.getAzzimovSearchInfo().getCount());
                //guidanceCategoryEntry.setResultCount(results - 13);
            }
        }
    }
}
