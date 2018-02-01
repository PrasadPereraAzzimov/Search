package com.azzimov.search.services.cache;

import com.azzimov.search.common.cache.couchbase.configurations.CouchbaseConfiguration;
import com.azzimov.search.common.cache.couchbase.configurations.CouchbaseConnector;
import com.azzimov.search.common.cache.couchbase.executors.CouchbaseCacheKeyListenerService;
import com.azzimov.search.common.cache.couchbase.executors.CouchbaseExecutor;
import com.azzimov.search.common.cache.providers.CacheServiceProvider;
import com.azzimov.search.common.cache.service.CacheService;
import com.azzimov.search.common.cache.service.ExecutorService;
import com.azzimov.search.common.util.config.SystemConfiguration;
import com.azzimov.search.listeners.ConfigListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by prasad on 2/1/18.
 * AzzimovCacheManager manages the cache connector and cache listener services
 */
@Component
public class AzzimovCacheManager {
    private ConfigListener configListener;
    private CouchbaseExecutor couchbaseExecutor = null;
    private CouchbaseConnector couchbaseConnector = null;
    private CouchbaseConfiguration couchbaseConfiguration = null;
    private CouchbaseCacheKeyListenerService couchbaseCacheKeyListenerService;
    private ExecutorService executorService = CouchbaseExecutor.getInstance();

    /**
     * Create all cache related components:
     * couchbaseConfiguration, couchbaseConnector, executorService, couchbaseCacheKeyListenerService
     * @return success of operation
     */
    boolean createAzzimovCacheExecutor() {
        List<Object> hostObjects = configListener.getConfigurationHandler()
                .getObjectConfigList(SystemConfiguration.COUCH_SERVER_NODES);
        List<Object> bucketObjects = configListener.getConfigurationHandler()
                .getObjectConfigList(SystemConfiguration.COUCH_BUCKETS);
        int couchbasePort = configListener.getConfigurationHandler().getIntConfig(SystemConfiguration.COUCH_CACHE_PORT);
        List<String> hosts = new ArrayList<>();
        List<String> buckets = new ArrayList<>();

        for (Object hostObject : hostObjects)
            hosts.add(hostObject.toString());
        for (Object bucketObject : bucketObjects)
            buckets.add(bucketObject.toString());

        couchbaseConfiguration = new
                CouchbaseConfiguration();
        couchbaseConfiguration.setHosts(hosts);
        couchbaseConfiguration.setBuckets(buckets);
        couchbaseConfiguration.setPort(couchbasePort);
        couchbaseConfiguration.setPassword("");
        CouchbaseConnector.start(couchbaseConfiguration);
        couchbaseConnector = CouchbaseConnector.getInstance();
        couchbaseExecutor = CouchbaseExecutor.getInstance();
        this.couchbaseCacheKeyListenerService = new CouchbaseCacheKeyListenerService(this.couchbaseConfiguration);
        this.couchbaseCacheKeyListenerService.initCacheListenerManager();
        this.executorService = CouchbaseExecutor.getInstance();
        return true;
    }

    /**
     * Stop/close all the cache base connections/listeners
     * @return success of operation
     */
    boolean closeAzzimovCacheExecutor() {
        CouchbaseConnector.closeConnection();
        CouchbaseConnector.closeBucket();
        this.couchbaseCacheKeyListenerService.shutdownCacheListenerManager();
        return true;
    }

    public CouchbaseExecutor getCouchbaseExecutor() {
        return couchbaseExecutor;
    }

    public void setCouchbaseExecutor(CouchbaseExecutor couchbaseExecutor) {
        this.couchbaseExecutor = couchbaseExecutor;
    }

    public CouchbaseCacheKeyListenerService getCouchbaseCacheKeyListenerService() {
        return couchbaseCacheKeyListenerService;
    }

    public <U> CacheService<U> getCacheProvider(Class<U> tClass) {
        return new CacheServiceProvider<>
                (executorService,tClass, this.couchbaseConnector);
    }

    @Autowired
    public void setConfigListener(ConfigListener configListener) {
        this.configListener = configListener;
    }
}
