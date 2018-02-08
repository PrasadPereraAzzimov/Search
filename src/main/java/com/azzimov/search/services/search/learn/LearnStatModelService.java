package com.azzimov.search.services.search.learn;

import com.azzimov.search.common.cache.requests.AzzimovCacheRequest;
import com.azzimov.search.common.cache.responses.AzzimovCacheResponse;
import com.azzimov.search.common.cache.service.CacheService;
import com.azzimov.search.common.dto.internals.feedback.FeedbackAttribute;
import com.azzimov.search.common.dto.internals.feedback.FeedbackAttributeLabel;
import com.azzimov.search.common.dto.internals.feedback.FeedbackAttributeNumericValue;
import com.azzimov.search.common.dto.internals.feedback.FeedbackAttributeStringValue;
import com.azzimov.search.common.util.config.SearchConfiguration;
import com.azzimov.search.listeners.ConfigListener;
import com.azzimov.search.services.cache.AzzimovCacheManager;
import com.azzimov.trinity.common.learning.ModelEntity;
import com.azzimov.trinity.common.learning.guidance.GuidanceModel;
import com.azzimov.trinity.common.learning.guidance.GuidanceModelElement;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by prasad on 2/1/18.
 * This component manages the statistic centroid models we create for search
 */
@Component
public class LearnStatModelService {
    private static final Logger logger = LogManager.getLogger(LearnStatModelService.class);
    private LearnCentroidCluster guidanceLearnCentroidCluster;
    private LearnCentroidCluster productLearnCentroidCluster;
    private AzzimovCacheManager azzimovCacheManager;
    private ConfigListener configListener;

    public LearnStatModelService() {
        this.guidanceLearnCentroidCluster = new LearnCentroidCluster();
        this.productLearnCentroidCluster = new LearnCentroidCluster();
    }

    public void updateGuidanceLearningModelManager() throws InvocationTargetException,
            NoSuchMethodException,
            InstantiationException,
            IllegalAccessException,
            IOException {
        double guidanceLearningMaxCoefficient = configListener.
                getConfigurationHandler().getDoubleConfig(SearchConfiguration.SEARCH_GUIDANCE_CENTROID_MAX_WEIGHT);
        CacheService<GuidanceModel> guidanceModelCacheService = azzimovCacheManager.getCacheProvider(GuidanceModel.class);
        // In in this version of Azzimov Search, we want to deploy only one bucket cache for all deployments..
        List<String> buckets = azzimovCacheManager.getCouchbaseConfiguration().getBuckets();
        String cacheName = buckets.get(0);
        AzzimovCacheRequest<GuidanceModel> azzimovCacheRequest = new AzzimovCacheRequest<>(
                cacheName, LearnCentroidCluster.CENTROID_GUIDANCE_KEY);
        AzzimovCacheResponse<GuidanceModel> azzimovCacheResponse = null;
        try {
            azzimovCacheResponse = guidanceModelCacheService.get(azzimovCacheRequest);
        } catch (NullPointerException e) {
            logger.warn("The cache objects for centroids not found : {} ...", LearnCentroidCluster.CENTROID_GUIDANCE_KEY);
        }
        if (azzimovCacheResponse != null) {
            GuidanceModel guidanceModel = azzimovCacheResponse.getObjectType();
            Map<String, Map<FeedbackAttribute, Float>> attributeCentroids = new HashMap<>();
            List<GuidanceModelElement> guidanceModelElementList = guidanceModel.getModelElementList();
            for (GuidanceModelElement guidanceModelElement : guidanceModelElementList) {
                List<ModelEntity> modelEntityList = guidanceModelElement.getModelEntityList();
                String modelKey = guidanceModelElement.getModelKey();
                attributeCentroids.put(modelKey, new HashMap<>());
                double sumValue = 0.0;
                for (ModelEntity modelEntity : modelEntityList) {
                    sumValue += Float.valueOf(String.valueOf(modelEntity.getValue())) *
                            Float.valueOf(String.valueOf(modelEntity.getFactor()));
                }
                // we adjust the total weights to be between range [0.1 - 0.01]
                int maxFactor  = (int) Math.log10(guidanceLearningMaxCoefficient);
                double curFactor = Math.log10(sumValue);
                double factor = Math.pow(10, (int)(maxFactor - curFactor));
                //log.debug("sumValue = {} curFactor = {} factor = {}", sumValue, curFactor, factor);
                for (ModelEntity modelEntity : modelEntityList) {
                    String attributeString = modelEntity.getLabel();
                    String[] attributeEntries = attributeString.split("::");
                    FeedbackAttribute feedbackAttribute = new FeedbackAttribute(
                            new FeedbackAttributeLabel(attributeEntries[0].trim()), 0);
                    if (attributeEntries.length <= 2) {
                        FeedbackAttributeStringValue feedbackAttributeStringValue =
                                new FeedbackAttributeStringValue(attributeEntries[1].trim());
                        feedbackAttribute.setFeedbackAttributeStringValue(feedbackAttributeStringValue);
                    } else {
                        Double value = Double.valueOf(attributeEntries[1].trim());
                        FeedbackAttributeNumericValue feedbackAttributeNumericValue =
                                new FeedbackAttributeNumericValue(value);
                        feedbackAttribute.setFeedbackAttributeNumericValue(feedbackAttributeNumericValue);
                        feedbackAttribute.setUnit(attributeEntries[2].trim());
                    }
                    attributeCentroids.get(modelKey).put(feedbackAttribute,
                            Float.valueOf(String.valueOf(modelEntity.getValue())) *
                                    Float.valueOf(String.valueOf(modelEntity.getFactor())) * (float) factor);
                }
            }
            logger.info("Updating the guidance based centroid model ...");
            guidanceLearnCentroidCluster.setAttributeCentroids(attributeCentroids);
        }
    }

    public LearnCentroidCluster getGuidanceLearnCentroidCluster() {
        return guidanceLearnCentroidCluster;
    }


    public LearnCentroidCluster getProductLearnCentroidCluster() {
        return productLearnCentroidCluster;
    }

    @Autowired
    public void setAzzimovCacheManager(AzzimovCacheManager azzimovCacheManager) {
        this.azzimovCacheManager = azzimovCacheManager;
    }

    @Autowired
    public void setConfigListener(ConfigListener configListener) {
        this.configListener = configListener;
    }
}
