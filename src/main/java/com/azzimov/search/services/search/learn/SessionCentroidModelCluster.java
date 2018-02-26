package com.azzimov.search.services.search.learn;


import com.azzimov.search.common.dto.internals.feedback.FeedbackAttribute;
import com.azzimov.search.system.actors.SessionLearningGeneratorActor;
import com.google.common.collect.Maps;
import org.joda.time.DateTime;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Session Learning Model creates a model that contains session related learned characteristics to improve search.
 */
public class SessionCentroidModelCluster extends LearnCentroidCluster {
    private DateTime creationTime;

    private Map<String, List<SessionCentroid>> sessionLearningModelClusterMap = Maps.newHashMap();

    public Map<String, List<SessionCentroid>> getSessionLearningModelClusterMap() {
        return sessionLearningModelClusterMap;
    }

    public void setSessionLearningModelClusterMap(
            Map<String, List<SessionCentroid>> sessionLearningModelClusterMap) {
        this.sessionLearningModelClusterMap = sessionLearningModelClusterMap;
    }

    public void accept(LearnCentroidClusterVisitor learnCentroidClusterVisitor) {
        learnCentroidClusterVisitor.visit(this);
    }

    public DateTime getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(DateTime creationTime) {
        this.creationTime = creationTime;
    }


    /**
     * Session Learning Entry that represents an entry for the session learning
     */
    public static class SessionCentroidEntry {
        private FeedbackAttribute feedbackAttribute;
        private SessionLearningGeneratorActor.SessionEntryType sessionEntryType;
        private float count;

        public FeedbackAttribute getFeedbackAttribute() {
            return feedbackAttribute;
        }

        public void setFeedbackAttribute(FeedbackAttribute feedbackAttribute) {
            this.feedbackAttribute = feedbackAttribute;
        }

        public SessionLearningGeneratorActor.SessionEntryType getSessionEntryType() {
            return sessionEntryType;
        }

        public void setSessionEntryType(SessionLearningGeneratorActor.SessionEntryType sessionEntryType) {
            this.sessionEntryType = sessionEntryType;
        }

        public float getCount() {
            return count;
        }

        public void setCount(float count) {
            this.count = count;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof SessionCentroidEntry)) return false;

            SessionCentroidEntry that = (SessionCentroidEntry) o;

            if (!feedbackAttribute.equals(that.feedbackAttribute)) return false;
            return sessionEntryType == that.sessionEntryType;

        }

        @Override
        public int hashCode() {
            int result = feedbackAttribute.hashCode();
            result = 31 * result + sessionEntryType.hashCode();
            return result;
        }
    }

    public static class SessionCentroid implements Comparator<SessionCentroid> {
        private Map<String, SessionCentroidEntry> keySessionLearningEntryMap = new HashMap<>();
        private Map<String, SessionCentroidEntry> sessionLearningEntryMap = new HashMap<>();
        private String clusterKey = "";
        private double weight;

        public String getClusterKey() {
            return clusterKey;
        }

        public void setClusterKey(String clusterKey) {
            this.clusterKey = clusterKey;
        }

        public Map<String, SessionCentroidEntry> getKeySessionLearningEntryMap() {
            return keySessionLearningEntryMap;
        }

        public void setKeySessionLearningEntryMap(Map<String, SessionCentroidEntry> keySessionLearningEntryMap) {
            this.keySessionLearningEntryMap = keySessionLearningEntryMap;
        }

        public Map<String, SessionCentroidEntry> getSessionLearningEntryMap() {
            return sessionLearningEntryMap;
        }

        public void setSessionLearningEntryMap(Map<String, SessionCentroidEntry> sessionLearningEntryMap) {
            this.sessionLearningEntryMap = sessionLearningEntryMap;
        }

        @Override
        public int compare(SessionCentroid s1, SessionCentroid s2) {
            int equals = 1;
            // when there's no key entries, we check if session entries are the same
            if (s1.keySessionLearningEntryMap.size() == s2.keySessionLearningEntryMap.size() &&
                    s1.keySessionLearningEntryMap.size() == 0) {
                equals = (sessionLearningEntryMap.size() == s2.sessionLearningEntryMap.size() &&
                        sessionLearningEntryMap.keySet().containsAll(s2.sessionLearningEntryMap.keySet())) ?
                        0 : 1;
            } else if (keySessionLearningEntryMap.size() == s2.keySessionLearningEntryMap.size()) {
                equals = keySessionLearningEntryMap.keySet().containsAll(s2.keySessionLearningEntryMap.keySet()) ?
                        0 : 1;
            }
            return equals;
        }

        public double getWeight() {
            return weight;
        }

        public void setWeight(double weight) {
            this.weight = weight;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof SessionCentroid)) return false;
            SessionCentroid that = (SessionCentroid) o;
            boolean equals = false;
            // when there's no key entries, we check if session entries are the same
            if (keySessionLearningEntryMap.size() == that.keySessionLearningEntryMap.size() &&
                    keySessionLearningEntryMap.size() == 0) {
                equals = (sessionLearningEntryMap.size() == that.sessionLearningEntryMap.size() &&
                        sessionLearningEntryMap.keySet().containsAll(that.sessionLearningEntryMap.keySet()));
            } else if (keySessionLearningEntryMap.size() == that.keySessionLearningEntryMap.size()) {
                equals = keySessionLearningEntryMap.keySet().containsAll(that.keySessionLearningEntryMap.keySet());
            }
            return equals;
        }

        @Override
        public int hashCode() {
            return keySessionLearningEntryMap.hashCode();
        }
    }
}