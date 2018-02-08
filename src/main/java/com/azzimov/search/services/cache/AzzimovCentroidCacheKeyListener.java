package com.azzimov.search.services.cache;

import com.azzimov.search.common.cache.listeners.CacheKeyListener;
import com.azzimov.search.common.cache.listeners.CacheListenerMessage;
import com.azzimov.search.services.search.learn.LearnCentroidCluster;
import com.azzimov.search.services.search.learn.LearnStatModelService;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

/**
 * Created by prasad on 2/1/18.
 * Listen to cache key changes to load new versions of centroids
 */
public class AzzimovCentroidCacheKeyListener implements CacheKeyListener {
    private LearnStatModelService learnStatModelService;

    @Override
    public void cacheKeyModified(CacheListenerMessage couchbaseMessage) {
        if (couchbaseMessage.getKey().equals(LearnCentroidCluster.CENTROID_GUIDANCE_KEY)) {
            try {
                learnStatModelService.updateGuidanceLearningModelManager();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void cacheKeyRemoved(CacheListenerMessage couchbaseMessage) {
        if (couchbaseMessage.getKey().equals("")) {
            // Nothing to do here for now
        }
    }

    public AzzimovCentroidCacheKeyListener(LearnStatModelService learnStatModelService) {
        this.learnStatModelService = learnStatModelService;
    }
}
