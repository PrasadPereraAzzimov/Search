package com.azzimov.search.services.search.learn;

import com.azzimov.search.common.dto.internals.feedback.FeedbackAttribute;
import com.azzimov.search.common.dto.internals.feedback.FeedbackCategory;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.Maps;

import java.io.Serializable;
import java.util.Map;

/**
 * Created by prasad on 2/1/18.
 * For now, since we need to port the old model to new, we will create these LearnCentroidCluster to manage the read
 * centroids
 */
public class LearnCentroidCluster implements Serializable {
    public static final String CENTROID_GUIDANCE_KEY = "guidance-learning-model";
    @JsonIgnore
    private Map<String, Map<FeedbackCategory, Float>> categoryCentroids = Maps.newConcurrentMap();
    @JsonIgnore
    private Map<String, Map<FeedbackAttribute, Float>> attributeCentroids = Maps.newConcurrentMap();


    public Map<String, Map<FeedbackCategory, Float>> getCategoryCentroids() {
        return categoryCentroids;
    }

    public void setCategoryCentroids(Map<String, Map<FeedbackCategory, Float>> categoryCentroids) {
        this.categoryCentroids = categoryCentroids;
    }

    public Map<String, Map<FeedbackAttribute, Float>> getAttributeCentroids() {
        return attributeCentroids;
    }

    public void setAttributeCentroids(Map<String, Map<FeedbackAttribute, Float>> attributeCentroids) {
        this.attributeCentroids = attributeCentroids;
    }

    public void accept(LearnCentroidClusterVisitor learnCentroidClusterVisitor) {
        learnCentroidClusterVisitor.visit(this);
    }

    public interface LearnCentroidClusterVisitor {
        void visit(LearnCentroidCluster learnCentroidCluster);
        void visit(SessionCentroidModelCluster sessionCentroidModelCluster);
    }
}
