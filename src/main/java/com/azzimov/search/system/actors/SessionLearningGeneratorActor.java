package com.azzimov.search.system.actors;

import akka.actor.AbstractActor;
import com.azzimov.search.common.cache.requests.AzzimovCacheRequest;
import com.azzimov.search.common.cache.responses.AzzimovCacheResponse;
import com.azzimov.search.common.cache.service.CacheService;
import com.azzimov.search.common.dto.GeoLocation;
import com.azzimov.search.common.dto.Language;
import com.azzimov.search.common.dto.LanguageCode;
import com.azzimov.search.common.dto.communications.requests.AzzimovUserRequestParameters;
import com.azzimov.search.common.dto.communications.requests.search.AzzimovSearchRequest;
import com.azzimov.search.common.dto.communications.requests.search.AzzimovSearchRequestParameters;
import com.azzimov.search.common.dto.communications.responses.search.AzzimovSearchResponse;
import com.azzimov.search.common.dto.externals.Attribute;
import com.azzimov.search.common.dto.externals.Guidance;
import com.azzimov.search.common.dto.externals.GuidanceFilter;
import com.azzimov.search.common.dto.externals.GuidanceFilterValue;
import com.azzimov.search.common.dto.externals.Product;
import com.azzimov.search.common.dto.externals.ProductGuidance;
import com.azzimov.search.common.dto.internals.ExtendedProduct;
import com.azzimov.search.common.dto.internals.feedback.Feedback;
import com.azzimov.search.common.dto.internals.feedback.FeedbackAttribute;
import com.azzimov.search.common.dto.internals.feedback.FeedbackAttributeLabel;
import com.azzimov.search.common.dto.internals.feedback.FeedbackAttributeNumericValue;
import com.azzimov.search.common.dto.internals.feedback.FeedbackAttributeStringValue;
import com.azzimov.search.common.dto.internals.feedback.GuidanceAttributeEntry;
import com.azzimov.search.common.dto.internals.feedback.GuidanceFeedback;
import com.azzimov.search.common.dto.internals.feedback.ProductFeedback;
import com.azzimov.search.common.dto.internals.feedback.QueryFeedback;
import com.azzimov.search.common.dto.internals.feedback.visitor.FeedbackVisitor;
import com.azzimov.search.common.query.AzzimovGetQuery;
import com.azzimov.search.common.requests.AzzimovGetRequest;
import com.azzimov.search.common.text.AzzimovTextProcessor;
import com.azzimov.search.common.text.AzzimovTextQuery;
import com.azzimov.search.common.util.config.ConfigurationHandler;
import com.azzimov.search.common.util.config.SearchConfiguration;
import com.azzimov.search.listeners.ConfigListener;
import com.azzimov.search.services.cache.AzzimovCacheManager;
import com.azzimov.search.services.feedback.AzzimovFeedbackPersistRequest;
import com.azzimov.search.services.search.executors.AzzimovAggregateExecutor;
import com.azzimov.search.services.search.executors.SearchExecutorService;
import com.azzimov.search.services.search.executors.product.AzzimovProductAggregateExecutorCreator;
import com.azzimov.search.services.search.learn.SessionCentroidModelCluster;
import com.azzimov.search.services.search.params.product.AzzimovSearchParameters;
import com.azzimov.search.services.search.validators.product.AzzimovSearchRequestValidator;
import io.netty.util.internal.StringUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static com.azzimov.search.services.search.reponses.AzzimovSearchResponseBuilder.PRODUCT_PREFIX;
import static com.azzimov.search.services.search.utils.SearchFieldConstants.VALUE_TEXT;
import static com.azzimov.search.system.spring.AppConfiguration.SESSION_LEARN_ACTOR;

/**
 * Created by prasad on 11/13/17.
 * SessionLearningGenerator Actor that handles generating session level model
 */
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Component(value = SESSION_LEARN_ACTOR)
public class SessionLearningGeneratorActor extends AbstractActor {
    // SessionLearningGenerator retrieve information on new sessions and it's respective feedbacks and regenerate the
    // feedback models. we temporarily write these session models to the cache and make them expire with time.
    private ConfigurationHandler configurationHandler;
    private AzzimovCacheManager azzimovCacheManager;
    private Map<SessionEntryType, Map<FeedbackAttribute, Float>> categoryAttributeStatistics = new HashMap<>();
    private static final Logger log = LogManager.getLogger(SessionLearningGeneratorActor.class);
    private SearchExecutorService searchExecutorService;
    public static final String SESSION_LEARNING_DEFAULT_CLUSTER_KEY = "ALL";
    public static final String DEFAULT_MODEL_KEY = "session-learning-centroid-model";


    public SessionLearningGeneratorActor(AzzimovCacheManager azzimovCacheManager,
                                         SearchExecutorService searchExecutorService,
                                         ConfigListener configListener) {
        this.azzimovCacheManager = azzimovCacheManager;
        this.searchExecutorService = searchExecutorService;
        this.configurationHandler = configListener.getConfigurationHandler();
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(AzzimovFeedbackPersistRequest.class, this::updateSessionModel).build();
    }

    @Override
    public void preStart() {
        log.info("Starting the session learning manager {} {}", getSelf(), getContext().props());
        retrieveAttributeStatistics();
        log.info("Retrieved relevant attributes for learning manager {}", getSelf());
    }


    /**
     * SessionEntryType session entry type that indicates categories, attributes
     */
    public enum SessionEntryType {
        CATEGORY_LEVEL1,
        CATEGORY_OTHERS,
        ATTRIBUTE_OTHERS
    }

    private void updateSessionModel(AzzimovFeedbackPersistRequest azzimovFeedbackPersistRequest) throws Exception {
        for (Feedback feedback : azzimovFeedbackPersistRequest.getFeedbackList()) {
            String cacheKey = configurationHandler
                    .getStringConfig(SearchConfiguration.SESSION_LEVEL_CENTROID_MODEL, DEFAULT_MODEL_KEY) +
                    feedback.getSessionId();
            // Retrieve previous model
            SessionCentroidModelCluster sessionCentroidModelClusterPrevious = retrieveSessionModel(cacheKey);
            SessionCentroidModelCluster sessionCentroidModelCluster = new SessionCentroidModelCluster();
            SessionLearningRefinementFeedbackVisitor sessionLearningFeedbackVisitor =
                    new SessionLearningRefinementFeedbackVisitor(this.searchExecutorService, this.configurationHandler,
                            this, azzimovFeedbackPersistRequest);
            SessionLearningQueryFeedbackVisitor sessionLearningQueryFeedbackVisitor =
                    new SessionLearningQueryFeedbackVisitor(sessionCentroidModelClusterPrevious);
            // retrieve all the refinements from the feedback
            List<SessionCentroidModelCluster.SessionCentroidEntry> sessionCentroidEntryList =
                    feedback.accept(sessionLearningFeedbackVisitor);
            String refinementSet = "[";
            for (SessionCentroidModelCluster.SessionCentroidEntry sessionCentroidEntry : sessionCentroidEntryList) {
                refinementSet += sessionCentroidEntry.getFeedbackAttribute().getFeedbackAttributeLabel().getLabel() + ",";
            }
            refinementSet += "]";
            // retrieve the query related to the feedback
            List<String> queryKeys = feedback.accept(sessionLearningQueryFeedbackVisitor);
            log.info("Refinement set = {} for the query = {}", refinementSet, queryKeys);
            Map<String, SessionCentroidModelCluster.SessionCentroidEntry> sessionLearningEntryMap = new HashMap<>();
            Map<String, List<SessionCentroidModelCluster.SessionCentroid>> sessionLearningModelClusterMap = new HashMap<>();

            for (String queryKey : queryKeys) {
                SessionCentroidModelCluster.SessionCentroid sessionCentroid =
                        new SessionCentroidModelCluster.SessionCentroid();
                for (SessionCentroidModelCluster.SessionCentroidEntry sessionCentroidEntry : sessionCentroidEntryList) {
                    String sessionLearningEntryKey = sessionCentroidEntry
                            .getFeedbackAttribute().getFeedbackAttributeLabel().getLabel();
                    if (sessionCentroidEntry.getFeedbackAttribute().getFeedbackAttributeStringValue() != null &&
                            !sessionCentroidEntry.getFeedbackAttribute().getFeedbackAttributeStringValue().getStrValue().isEmpty()) {
                        sessionLearningEntryKey += ProductGuidance.PRODUCT_GUIDANCE_SEPARATOR +
                                sessionCentroidEntry.getFeedbackAttribute().getFeedbackAttributeStringValue().getStrValue();
                    } else {
                        sessionLearningEntryKey += ProductGuidance.PRODUCT_GUIDANCE_SEPARATOR +
                                sessionCentroidEntry.getFeedbackAttribute()
                                        .getFeedbackAttributeNumericValue().getNumericValue()
                                + ProductGuidance.PRODUCT_GUIDANCE_SEPARATOR +
                                sessionCentroidEntry.getFeedbackAttribute().getUnit();
                    }

                    if (sessionCentroidEntry.getSessionEntryType() == SessionEntryType.CATEGORY_LEVEL1 ||
                            sessionCentroidEntry.getSessionEntryType() == SessionEntryType.CATEGORY_OTHERS) {
                        sessionCentroid.getKeySessionLearningEntryMap()
                                .put(sessionLearningEntryKey, sessionCentroidEntry);
                    } else {
                        sessionLearningEntryMap.put(sessionLearningEntryKey, sessionCentroidEntry);
                    }
                }
                sessionCentroid.setClusterKey(feedback.getRequestId());
                sessionCentroid.setSessionLearningEntryMap(sessionLearningEntryMap);
                List<SessionCentroidModelCluster.SessionCentroid> sessionCentroidList = new ArrayList<>();
                sessionCentroidList.add(sessionCentroid);
                sessionLearningModelClusterMap.put(queryKey, sessionCentroidList);
            }
            sessionCentroidModelCluster.setSessionLearningModelClusterMap(sessionLearningModelClusterMap);

            // create a all type cluster just with attributes
            SessionCentroidModelCluster.SessionCentroid sessionCentroid =
                    new SessionCentroidModelCluster.SessionCentroid();
            sessionCentroid.setClusterKey(feedback.getRequestId());
            sessionLearningEntryMap = new HashMap<>();
            for (SessionCentroidModelCluster.SessionCentroidEntry sessionCentroidEntry : sessionCentroidEntryList) {
                String sessionLearningEntryKey = sessionCentroidEntry
                        .getFeedbackAttribute().getFeedbackAttributeLabel().getLabel();
                if (sessionCentroidEntry.getFeedbackAttribute().getFeedbackAttributeStringValue() != null &&
                        !sessionCentroidEntry.getFeedbackAttribute().getFeedbackAttributeStringValue().getStrValue().isEmpty()) {
                    sessionLearningEntryKey += ProductGuidance.PRODUCT_GUIDANCE_SEPARATOR +
                            sessionCentroidEntry.getFeedbackAttribute().getFeedbackAttributeStringValue().getStrValue();
                } else {
                    sessionLearningEntryKey += ProductGuidance.PRODUCT_GUIDANCE_SEPARATOR +
                            sessionCentroidEntry.getFeedbackAttribute()
                                    .getFeedbackAttributeNumericValue().getNumericValue()
                            + ProductGuidance.PRODUCT_GUIDANCE_SEPARATOR +
                            sessionCentroidEntry.getFeedbackAttribute().getUnit();
                }
                if (sessionCentroidEntry.getSessionEntryType() != SessionEntryType.CATEGORY_LEVEL1 &&
                        sessionCentroidEntry.getSessionEntryType() != SessionEntryType.CATEGORY_OTHERS) {
                    sessionLearningEntryMap.put(sessionLearningEntryKey, sessionCentroidEntry);
                }
            }
            sessionCentroid.setSessionLearningEntryMap(sessionLearningEntryMap);
            List<SessionCentroidModelCluster.SessionCentroid> sessionCentroidListAll = new ArrayList<>();
            sessionCentroidListAll.add(sessionCentroid);
            sessionCentroidModelCluster.getSessionLearningModelClusterMap().put(SESSION_LEARNING_DEFAULT_CLUSTER_KEY,
                    sessionCentroidListAll);

            SessionCentroidModelCluster sessionCentroidModelClusterNext = mergeSessionLearningModels(sessionCentroidModelCluster,
                    sessionCentroidModelClusterPrevious);
            sessionCentroidModelClusterNext.setCreationTime(DateTime.now(DateTimeZone.UTC));
            String bucket = azzimovCacheManager.getCouchbaseConfiguration().getBuckets().get(0);
            AzzimovCacheRequest<SessionCentroidModelCluster> azzimovCacheRequest =
                    new AzzimovCacheRequest<>(bucket, cacheKey);
            azzimovCacheRequest.setExpiration(1000);
            azzimovCacheRequest.setObjectType(sessionCentroidModelClusterNext);
            CacheService<SessionCentroidModelCluster> learningModelCacheService =
                    azzimovCacheManager.getCacheProvider(SessionCentroidModelCluster.class);
            learningModelCacheService.add(azzimovCacheRequest);
        }
    }

    /**
     * Retrieve session model currently in the cache for the given session
     *
     * @param cacheKey key for the cache
     * @return Session model current
     * @throws Exception exception that can be thrown
     */
    private SessionCentroidModelCluster retrieveSessionModel(String cacheKey) throws Exception {
        // So far, we want to use one bucket for all
        String bucket = azzimovCacheManager.getCouchbaseConfiguration().getBuckets().get(0);
        AzzimovCacheRequest<SessionCentroidModelCluster> azzimovCacheRequest =
                new AzzimovCacheRequest<>(bucket, cacheKey);
        CacheService<SessionCentroidModelCluster> learningModelCacheService =
                azzimovCacheManager.getCacheProvider(SessionCentroidModelCluster.class);
        AzzimovCacheResponse<SessionCentroidModelCluster> azzimovCacheResponse = learningModelCacheService.get(azzimovCacheRequest);
        SessionCentroidModelCluster sessionCentroidModelClusterPrevious;
        if (azzimovCacheResponse.getObjectType() != null) {
            sessionCentroidModelClusterPrevious = azzimovCacheResponse.getObjectType();
        } else {
            sessionCentroidModelClusterPrevious = new SessionCentroidModelCluster();
            Map<String, List<SessionCentroidModelCluster.SessionCentroid>> sessionLearningModelClusterMap = new HashMap<>();
            sessionCentroidModelClusterPrevious.setSessionLearningModelClusterMap(sessionLearningModelClusterMap);
            sessionCentroidModelClusterPrevious.setCreationTime(DateTime.now(DateTimeZone.UTC));
        }
        return sessionCentroidModelClusterPrevious;
    }

    /**
     * Merge two models, current and new model generated
     *
     * @param sessionCentroidModelClusterCurrent  current model generated
     * @param sessionCentroidModelClusterPrevious previous/existing model
     * @return Next session model generated
     */
    private SessionCentroidModelCluster mergeSessionLearningModels(SessionCentroidModelCluster sessionCentroidModelClusterCurrent,
                                                                   SessionCentroidModelCluster sessionCentroidModelClusterPrevious) {
        // Update previous model based on time
        sessionCentroidModelClusterPrevious = updatePreviousModel(sessionCentroidModelClusterPrevious);

        Map<String, List<SessionCentroidModelCluster.SessionCentroid>> sessionLearningModelClusterMapCurrent =
                sessionCentroidModelClusterCurrent.getSessionLearningModelClusterMap();
        Map<String, List<SessionCentroidModelCluster.SessionCentroid>> sessionLearningModelClusterMapPrevious =
                sessionCentroidModelClusterPrevious.getSessionLearningModelClusterMap();

        for (Map.Entry<String, List<SessionCentroidModelCluster.SessionCentroid>> entry :
                sessionLearningModelClusterMapCurrent.entrySet()) {
            if (sessionLearningModelClusterMapPrevious.containsKey(entry.getKey())) {
                List<SessionCentroidModelCluster.SessionCentroid> sessionCentroidCurrentList = entry.getValue();
                List<SessionCentroidModelCluster.SessionCentroid> sessionCentroidPreviousList =
                        sessionLearningModelClusterMapPrevious.get(entry.getKey());
                for (SessionCentroidModelCluster.SessionCentroid sessionCentroidCurrent :
                        sessionCentroidCurrentList) {
                    if (sessionCentroidPreviousList.contains(sessionCentroidCurrent)) {
                        // merge the clusters
                        SessionCentroidModelCluster.SessionCentroid sessionCentroidPrevious =
                                sessionCentroidPreviousList
                                        .get(sessionCentroidPreviousList.indexOf(sessionCentroidCurrent));
                        for (Map.Entry<String, SessionCentroidModelCluster.SessionCentroidEntry> sessionEntry :
                                sessionCentroidCurrent.getSessionLearningEntryMap().entrySet()) {
                            if (sessionCentroidPrevious.getSessionLearningEntryMap().containsKey(sessionEntry.getKey())) {
                                sessionCentroidPrevious.getSessionLearningEntryMap()
                                        .get(sessionEntry.getKey()).setCount(
                                        sessionCentroidPrevious.getSessionLearningEntryMap()
                                                .get(sessionEntry.getKey()).getCount() +
                                                sessionEntry.getValue().getCount());
                            } else {
                                sessionCentroidPrevious.getSessionLearningEntryMap()
                                        .put(sessionEntry.getKey(), sessionEntry.getValue());
                            }
                        }
                    } else {
                        sessionCentroidPreviousList.add(sessionCentroidCurrent);
                    }
                    // For all the existing clusters, update the request session key
                    for (SessionCentroidModelCluster.SessionCentroid sessionCentroid :
                            sessionCentroidPreviousList) {
                        sessionCentroid.setClusterKey(sessionCentroidCurrent.getClusterKey());
                    }
                }
            } else {
                sessionLearningModelClusterMapPrevious.put(entry.getKey(), entry.getValue());
            }
        }
        SessionCentroidModelCluster sessionCentroidModelCluster = new SessionCentroidModelCluster();
        sessionCentroidModelCluster.setSessionLearningModelClusterMap(sessionLearningModelClusterMapPrevious);
        // remove the clusters which passes the minimum weight limit
        dropMinimalClusters(sessionCentroidModelCluster);
        return reWeightTheSessionModel(sessionCentroidModelCluster);
    }

    private SessionCentroidModelCluster updatePreviousModel(SessionCentroidModelCluster sessionCentroidModelCluster) {
        DateTime dateTime = sessionCentroidModelCluster.getCreationTime().toDateTime(DateTimeZone.UTC);
        double timeDecayFactor = configurationHandler.
                getIntConfig(SearchConfiguration.SESSION_LEVEL_CENTROID_DECAY, 600);
        double decayFactor = Math.exp((dateTime.getSecondOfDay() - DateTime.now(DateTimeZone.UTC).getSecondOfDay())
                / timeDecayFactor);
        for (Map.Entry<String, List<SessionCentroidModelCluster.SessionCentroid>> entry :
                sessionCentroidModelCluster.getSessionLearningModelClusterMap().entrySet()) {
            for (SessionCentroidModelCluster.SessionCentroid sessionCentroid : entry.getValue()) {
                for (SessionCentroidModelCluster.SessionCentroidEntry sessionCentroidEntry :
                        sessionCentroid.getSessionLearningEntryMap().values()) {
                    sessionCentroidEntry.setCount(sessionCentroidEntry.getCount() * (float) decayFactor);
                }
            }
        }
        return sessionCentroidModelCluster;
    }

    private void dropMinimalClusters(SessionCentroidModelCluster sessionCentroidModelCluster) {
        Iterator<Map.Entry<String, List<SessionCentroidModelCluster.SessionCentroid>>> entryIterator =
                sessionCentroidModelCluster.getSessionLearningModelClusterMap().entrySet().iterator();
        while (entryIterator.hasNext()) {
            Map.Entry<String, List<SessionCentroidModelCluster.SessionCentroid>> entry = entryIterator.next();
            Iterator<SessionCentroidModelCluster.SessionCentroid> clusterIterator = entry.getValue().iterator();
            double max = 0.0;
            while (clusterIterator.hasNext()) {
                SessionCentroidModelCluster.SessionCentroid sessionCentroid = clusterIterator.next();
                double weight = 0.0;
                for (SessionCentroidModelCluster.SessionCentroidEntry sessionCentroidEntry :
                        sessionCentroid.getSessionLearningEntryMap().values()) {
                    weight += sessionCentroidEntry.getCount();
                }
                weight = weight / Math.max(1, sessionCentroid.getSessionLearningEntryMap().values().size() - 1);
                if (weight > max) {
                    max = weight;
                }
            }
            // drop the ones with lower ratio but not zero
            clusterIterator = entry.getValue().iterator();
            while (clusterIterator.hasNext()) {
                SessionCentroidModelCluster.SessionCentroid sessionCentroid = clusterIterator.next();
                double weight = 0.0;
                for (SessionCentroidModelCluster.SessionCentroidEntry sessionCentroidEntry :
                        sessionCentroid.getSessionLearningEntryMap().values()) {
                    weight += sessionCentroidEntry.getCount();
                }
                int weightRatioLimit = configurationHandler.
                        getIntConfig(SearchConfiguration.SESSION_LEVEL_PRODUCT_FACTOR, 2);
                weight = weight / Math.max(1, sessionCentroid.getSessionLearningEntryMap().size() - 1);
                if (weight > 0.0 && max / weight >= weightRatioLimit) {
                    clusterIterator.remove();
                }
            }
        }
    }

    private SessionCentroidModelCluster reWeightTheSessionModel(SessionCentroidModelCluster sessionCentroidModelCluster) {
        for (Map.Entry<String, List<SessionCentroidModelCluster.SessionCentroid>> entry :
                sessionCentroidModelCluster.getSessionLearningModelClusterMap().entrySet()) {
            List<Float> avgClusterWeights = new ArrayList<>();
            // calculate avg weight on all clusters
            for (SessionCentroidModelCluster.SessionCentroid sessionCentroid : entry.getValue()) {
                float avgWeight = 0;
                for (SessionCentroidModelCluster.SessionCentroidEntry sessionCentroidEntry :
                        sessionCentroid.getSessionLearningEntryMap().values()) {
                    avgWeight += sessionCentroidEntry.getCount();
                }
                float div = Math.max(1.0f, sessionCentroid.getSessionLearningEntryMap().size() - 1);
                avgWeight = avgWeight / div;
                avgClusterWeights.add(avgWeight);
            }
            double avgWeightSqrt = 1.0f;
            for (Float avgWeight : avgClusterWeights) {
                avgWeightSqrt += avgWeight * avgWeight;
            }
            avgWeightSqrt = Math.sqrt(avgWeightSqrt);
            int index = 0;
            // Adjust weights compared to lowest value
            double sumValue = 0.0;
            for (Float avgWeight : avgClusterWeights) {
                entry.getValue().get(index++).setWeight(avgWeight / avgWeightSqrt);
                sumValue += (avgWeight / avgWeightSqrt);
            }

            // we adjust the total weights to be between range [0.1 - 0.01]
            double sessionLearningMaxWeight = configurationHandler.
                    getDoubleConfig(SearchConfiguration.SESSION_LEVEL_CLUSTER_MAX_WEIGHT, 0.01);
            int maxFactor = (int) Math.log10(sessionLearningMaxWeight);
            double curFactor = Math.log10(sumValue);
            double factor = Math.pow(10, (int) (maxFactor - curFactor));
            if (factor > 0.0) {
                for (SessionCentroidModelCluster.SessionCentroid sessionCentroid : entry.getValue()) {
                    sessionCentroid.setWeight(sessionCentroid.getWeight() * factor);
                }
            }
        }
        return sessionCentroidModelCluster;
    }

    public void retrieveAttributeStatistics() {
        AzzimovAggregateExecutor azzimovAggregateExecutor = new AzzimovProductAggregateExecutorCreator(
                configurationHandler,
                searchExecutorService,
                azzimovCacheManager);

        double maxAttributeFr = configurationHandler.
                getDoubleConfig(SearchConfiguration.SESSION_ATTRIBUTE_MAX_FREQUENT, 0.05);
        double minAttributeFr = configurationHandler.
                getDoubleConfig(SearchConfiguration.SESSION_ATTRIBUTE_MIN_FREQUENT, 0.0005);
        List<AzzimovSearchParameters> azzimovSearchParametersList = new ArrayList<>();
        AzzimovSearchRequestValidator azzimovSearchRequestValidator =
                new AzzimovSearchRequestValidator(this.configurationHandler);
        AzzimovSearchRequest azzimovSearchRequest = createInternalSearchRequest();
        AzzimovSearchParameters azzimovSearchParameters = azzimovSearchRequestValidator
                .validateRequest(azzimovSearchRequest);
        azzimovSearchParametersList.add(azzimovSearchParameters);
        try {
            List<AzzimovSearchResponse> azzimovSearchResponseList =
                    azzimovAggregateExecutor.aggregate(azzimovSearchParametersList);
            Guidance guidance = azzimovSearchResponseList.get(0).getAzzimovSearchResponseParameter().getGuidance();
            long totalDocs = azzimovSearchResponseList.get(0).getAzzimovSearchInfo().getCount();
            // Lets calculate fr stats and put in map
            categoryAttributeStatistics.put(SessionEntryType.ATTRIBUTE_OTHERS,
                    new TreeMap<>((f1, f2) -> {
                        if (!f1.getFeedbackAttributeLabel().getLabel()
                                .equals(f2.getFeedbackAttributeLabel().getLabel())) {
                            return f1.getFeedbackAttributeLabel().getLabel()
                                    .compareTo(f2.getFeedbackAttributeLabel().getLabel());
                        } else {
                            if (f1.getFeedbackAttributeStringValue() != null &&
                                    f2.getFeedbackAttributeStringValue() != null) {
                                return f1.getFeedbackAttributeStringValue().getStrValue()
                                        .compareTo(f2.getFeedbackAttributeStringValue().getStrValue());
                            } else if (f1.getFeedbackAttributeNumericValue() != null &&
                                    f2.getFeedbackAttributeNumericValue() != null) {
                                return Double.valueOf(f1.getFeedbackAttributeNumericValue().getNumericValue())
                                        .compareTo(f2.getFeedbackAttributeNumericValue().getNumericValue());
                            } else {
                                return 1;
                            }
                        }
                    }));
            for (GuidanceFilter guidanceFilter : guidance.getGuidanceFilters()) {
                FeedbackAttributeLabel feedbackAttributeLabel = new FeedbackAttributeLabel(guidanceFilter.getLabel());
                String filterType = guidanceFilter.getFilterType();
                for (GuidanceFilterValue guidanceFilterValue : guidanceFilter.getGuidanceFilterValues()) {
                    FeedbackAttribute feedbackAttribute = new FeedbackAttribute(feedbackAttributeLabel, 0);
                    String filterValue = guidanceFilterValue.getValue();
                    long filterCount = guidanceFilterValue.getValueCount();
                    if (!filterType.equals(VALUE_TEXT)) {
                        try {
                            double attributeNumValue = Double.parseDouble(filterValue);
                            FeedbackAttributeNumericValue feedbackAttributeNumericValue =
                                    new FeedbackAttributeNumericValue(attributeNumValue);
                            feedbackAttribute.setFeedbackAttributeNumericValue(feedbackAttributeNumericValue);
                        } catch (NumberFormatException e) {
                            FeedbackAttributeStringValue feedbackAttributeStringValue =
                                    new FeedbackAttributeStringValue(filterValue);
                            feedbackAttribute.setFeedbackAttributeStringValue(feedbackAttributeStringValue);
                            feedbackAttribute.setUnit(filterType);
                        }
                        feedbackAttribute.setUnit(filterType);

                    } else {
                        FeedbackAttributeStringValue feedbackAttributeStringValue =
                                new FeedbackAttributeStringValue(filterValue);
                        feedbackAttribute.setFeedbackAttributeStringValue(feedbackAttributeStringValue);
                        feedbackAttribute.setUnit(filterType);
                    }
                    float fr = (float) filterCount / totalDocs;
                    if (fr > minAttributeFr && fr < maxAttributeFr)
                        categoryAttributeStatistics.get(SessionEntryType.ATTRIBUTE_OTHERS).put(feedbackAttribute, fr);
                }
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        }
    }

    private AzzimovSearchRequest createInternalSearchRequest() {
        AzzimovSearchRequest azzimovSearchRequest = new AzzimovSearchRequest();
        AzzimovSearchRequestParameters azzimovSearchRequestParameters = new AzzimovSearchRequestParameters();
        List<String> targetDocs = new ArrayList<>();
        targetDocs.add(Product.PRODUCT_EXTERNAL_NAME);
        azzimovSearchRequestParameters.setDocumentTypes(targetDocs);
        azzimovSearchRequestParameters.setQuery(StringUtil.EMPTY_STRING);
        azzimovSearchRequestParameters.setResultsPerPage(1);
        azzimovSearchRequestParameters.setResultOffset(0);
        azzimovSearchRequestParameters.setLanguage(new Language(LanguageCode.EN_US));
        azzimovSearchRequestParameters.setAzzimovRequestFilters(new ArrayList<>());
        AzzimovUserRequestParameters azzimovUserRequestParameters = new AzzimovUserRequestParameters();
        azzimovUserRequestParameters.setBrowserDevice("Device");
        azzimovUserRequestParameters.setGeoLocation(new GeoLocation(1.0, 1.0));
        azzimovUserRequestParameters.setMemberId("member_default");
        azzimovUserRequestParameters.setRequestId("request_id");
        azzimovUserRequestParameters.setSessionId("session_id");
        azzimovUserRequestParameters.setUserType("user_default");
        azzimovSearchRequest.setAzzimovSearchRequestParameters(azzimovSearchRequestParameters);
        azzimovSearchRequest.setAzzimovUserRequestParameters(azzimovUserRequestParameters);
        return azzimovSearchRequest;
    }

    private static class SessionLearningRefinementFeedbackVisitor
            implements FeedbackVisitor<List<SessionCentroidModelCluster.SessionCentroidEntry>> {
        private SearchExecutorService searchExecutorService;
        private ConfigurationHandler configurationHandler;
        private SessionLearningGeneratorActor sessionLearningGeneratorActor;

        public SessionLearningRefinementFeedbackVisitor(SearchExecutorService searchExecutorService,
                                                        ConfigurationHandler configurationHandler,
                                                        SessionLearningGeneratorActor sessionLearningGeneratorActor,
                                                        AzzimovFeedbackPersistRequest azzimovFeedbackPersistRequest) {
            this.searchExecutorService = searchExecutorService;
            this.configurationHandler = configurationHandler;
            this.sessionLearningGeneratorActor = sessionLearningGeneratorActor;
        }

        @Override
        public List<SessionCentroidModelCluster.SessionCentroidEntry> visit(QueryFeedback queryFeedback) throws Exception {
            // Nothing to do here.
            return new ArrayList<>();
        }

        @Override
        public List<SessionCentroidModelCluster.SessionCentroidEntry> visit(ProductFeedback productFeedback) throws Exception {
            List<SessionCentroidModelCluster.SessionCentroidEntry> sessionCentroidEntryList = new ArrayList<>();
            // When the feedback is product view type, retrieve the product dto, specially the categories/attributes
            long productId = productFeedback.getProductId();
            ExtendedProduct extendedProduct = retrieveProductViewed(productId);
            com.azzimov.search.common.dto.Language language = productFeedback.getLanguage();
            Product product = new Product();
            List<ExtendedProduct> extendedProductList = new ArrayList<>();
            extendedProductList.add(extendedProduct);

            List<Product> productList = product.toExternal(extendedProductList, language);
            product = productList.get(0);
            List<Attribute> attributeList = product.getAttributes();
            for (Attribute attribute : attributeList) {
                if (attribute.getLabel() != null && !attribute.getLabel().isEmpty()) {
                    if (attribute.getStrValues() != null && !attribute.getStrValues().isEmpty()) {
                        for (String attributeValue : attribute.getStrValues()) {
                            FeedbackAttributeLabel feedbackAttributeLabel = new FeedbackAttributeLabel(attribute.getLabel());
                            FeedbackAttribute feedbackAttribute = new FeedbackAttribute(feedbackAttributeLabel, attribute.getId());
                            FeedbackAttributeStringValue feedbackAttributeStringValue =
                                    new FeedbackAttributeStringValue(attributeValue);
                            feedbackAttribute.setFeedbackAttributeStringValue(feedbackAttributeStringValue);
                            if (validateRefinementEntry(SessionEntryType.ATTRIBUTE_OTHERS, feedbackAttribute)) {
                                SessionCentroidModelCluster.SessionCentroidEntry sessionLearningEntry =
                                        new SessionCentroidModelCluster.SessionCentroidEntry();
                                sessionLearningEntry.setFeedbackAttribute(feedbackAttribute);
                                sessionLearningEntry.setSessionEntryType(SessionEntryType.ATTRIBUTE_OTHERS);
                                sessionCentroidEntryList.add(sessionLearningEntry);
                            }
                        }
                    } else {
                        for (Double attributeValue : attribute.getNumValues()) {
                            FeedbackAttributeLabel feedbackAttributeLabel = new FeedbackAttributeLabel(attribute.getLabel());
                            FeedbackAttribute feedbackAttribute = new FeedbackAttribute(feedbackAttributeLabel, attribute.getId());

                            FeedbackAttributeNumericValue feedbackAttributeNumericValue =
                                    new FeedbackAttributeNumericValue(attributeValue);
                            feedbackAttribute.setUnit(attribute.getUnit());
                            feedbackAttribute.setFeedbackAttributeNumericValue(feedbackAttributeNumericValue);
                            if (validateRefinementEntry(SessionEntryType.ATTRIBUTE_OTHERS, feedbackAttribute)) {
                                SessionCentroidModelCluster.SessionCentroidEntry sessionLearningEntry =
                                        new SessionCentroidModelCluster.SessionCentroidEntry();
                                sessionLearningEntry.setFeedbackAttribute(feedbackAttribute);
                                sessionLearningEntry.setSessionEntryType(SessionEntryType.ATTRIBUTE_OTHERS);
                                sessionCentroidEntryList.add(sessionLearningEntry);
                            }
                        }
                    }
                }
            }
            return sessionCentroidEntryList;
        }

        @Override
        public List<SessionCentroidModelCluster.SessionCentroidEntry> visit(GuidanceFeedback guidanceFeedback) throws Exception {
            List<SessionCentroidModelCluster.SessionCentroidEntry> sessionCentroidEntryList = new ArrayList<>();
            int guidanceFactor = configurationHandler.getIntConfig(SearchConfiguration.SESSION_LEVEL_REFINEMENT_FACTOR, 5);
            for (GuidanceAttributeEntry guidanceAttributeEntry : guidanceFeedback.getGuidanceAttributeEntries()) {
                FeedbackAttribute feedbackAttribute = guidanceAttributeEntry.getFeedbackAttribute();
                SessionCentroidModelCluster.SessionCentroidEntry sessionCentroidEntry = new SessionCentroidModelCluster.SessionCentroidEntry();
                sessionCentroidEntry.setFeedbackAttribute(feedbackAttribute);
                sessionCentroidEntry.setSessionEntryType(SessionEntryType.ATTRIBUTE_OTHERS);
                sessionCentroidEntry.setCount(guidanceFactor);
                sessionCentroidEntryList.add(sessionCentroidEntry);
            }
            return sessionCentroidEntryList;
        }

        /**
         * Retrieve product which was viewed
         *
         * @param productId product id to retrieve
         * @return Product reuqested
         * @throws IOException throws IOException
         */
        private ExtendedProduct retrieveProductViewed(long productId)
                throws IOException, IllegalAccessException, InstantiationException {
            AzzimovSearchRequest azzimovSearchRequest = sessionLearningGeneratorActor.createInternalSearchRequest();
            AzzimovSearchRequestValidator azzimovSearchRequestValidator =
                    new AzzimovSearchRequestValidator(this.configurationHandler);
            AzzimovSearchParameters azzimovSearchParameters = azzimovSearchRequestValidator
                    .validateRequest(azzimovSearchRequest);

            Map<String, String> targetDocumentTypes = azzimovSearchParameters.getTargetRepositories();
            String targetRepository = targetDocumentTypes.get(Product.PRODUCT_EXTERNAL_NAME);
            // pre process the query as we want here
            List<String> targetDocs = new ArrayList<>();
            targetDocs.add(Product.PRODUCT_EXTERNAL_NAME);
            AzzimovGetRequest<ExtendedProduct> azzimovGetRequest = new AzzimovGetRequest<>(ExtendedProduct.class);
            AzzimovGetQuery azzimovGetQuery = new AzzimovGetQuery(targetRepository, targetDocs);
            List<String> productIds = new ArrayList<>();
            productIds.add(PRODUCT_PREFIX + String.valueOf(productId));
            azzimovGetQuery.setDocumentIds(productIds);
            azzimovGetRequest.setAzzimovGetQuery(azzimovGetQuery);
            azzimovGetRequest.setClazz(ExtendedProduct.class);
            List<ExtendedProduct> extendedProductList =
                    searchExecutorService.getExecutorService().performGetRequest(azzimovGetRequest).getObjectType();
            return extendedProductList.get(0);
        }

        private boolean validateRefinementEntry(SessionEntryType sessionEntryType,
                                                FeedbackAttribute feedbackAttribute) {
            if (sessionLearningGeneratorActor.categoryAttributeStatistics.get(sessionEntryType)
                    .containsKey(feedbackAttribute)) {
                return true;
            }
            return false;
        }
    }

    private static class SessionLearningQueryFeedbackVisitor implements FeedbackVisitor<List<String>> {
        private SessionCentroidModelCluster sessionCentroidModelCluster;
        private static final int MAX_N_GRAM_LIMIT = 5;
        private static final int MIN_N_GRAM_LIMIT = 2;

        public SessionLearningQueryFeedbackVisitor(SessionCentroidModelCluster sessionCentroidModelCluster) {
            this.sessionCentroidModelCluster = sessionCentroidModelCluster;
        }

        @Override
        public List<String> visit(QueryFeedback queryFeedback) throws Exception {
            String query = queryFeedback.getQuery();
            AzzimovTextProcessor azzimovTextProcessor = new AzzimovTextProcessor();
            List<String> normalizedQueries = new ArrayList<>();
            try {
                Set<AzzimovTextQuery> azzimovTextQueries = azzimovTextProcessor.retrieveNGramTokenizedQueries(query,
                        queryFeedback.getLanguage().getLocale(),
                        new ArrayList<>(),
                        MIN_N_GRAM_LIMIT,
                        MAX_N_GRAM_LIMIT);
                for (AzzimovTextQuery azzimovTextQuery : azzimovTextQueries) {
                    normalizedQueries.add(azzimovTextQuery.getProcessedQueryString());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return normalizedQueries;
        }

        @Override
        public List<String> visit(ProductFeedback productFeedback) throws Exception {
            String requestId = productFeedback.getRequestId();
            List<String> queryList = new ArrayList<>();
            for (Map.Entry<String, List<SessionCentroidModelCluster.SessionCentroid>> entry :
                    this.sessionCentroidModelCluster.getSessionLearningModelClusterMap().entrySet()) {
                List<SessionCentroidModelCluster.SessionCentroid> sessionCentroidList = entry.getValue();
                for (SessionCentroidModelCluster.SessionCentroid sessionCentroid : sessionCentroidList) {
                    if (sessionCentroid.getClusterKey().equals(requestId)) {
                        queryList.add(entry.getKey());
                    }
                }
            }
            return queryList;
        }

        @Override
        public List<String> visit(GuidanceFeedback guidanceFeedback) throws Exception {
            String query = guidanceFeedback.getQuery();
            AzzimovTextProcessor azzimovTextProcessor = new AzzimovTextProcessor();
            List<String> normalizedQueries = new ArrayList<>();
            try {
                Set<AzzimovTextQuery> azzimovTextQueries = azzimovTextProcessor.retrieveNGramTokenizedQueries(query,
                        guidanceFeedback.getLanguage().getLocale(),
                        new ArrayList<>(),
                        MIN_N_GRAM_LIMIT,
                        MAX_N_GRAM_LIMIT);
                for (AzzimovTextQuery azzimovTextQuery : azzimovTextQueries) {
                    normalizedQueries.add(azzimovTextQuery.getProcessedQueryString());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return normalizedQueries;
        }
    }
}
