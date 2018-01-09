package com.azzimov.search.services;

import com.azzimov.search.common.elasticsearch.configurations.ElasticsearchConfiguration;
import com.azzimov.search.common.elasticsearch.executors.ElasticsearchExecutorService;
import com.azzimov.search.common.util.config.SystemConfiguration;
import com.azzimov.search.listeners.ConfigListener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by prasad on 1/5/18.
 * Search Executor service which provides the executor service for underlying no-sql/IR platform
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
public class SearchExecutorService {
    private static final Logger logger = LogManager.getLogger(SearchExecutorService.class);
    private final ElasticsearchExecutorService elasticsearchExecutorService;

    public SearchExecutorService(@Autowired ConfigListener configListener) {
        elasticsearchExecutorService = initExecutorService(configListener);
    }

    private ElasticsearchExecutorService initExecutorService(ConfigListener configListener) {
        String clusterName = configListener.getConfigurationHandler()
                .getStringConfig(SystemConfiguration.ES_CLUSTER_NAME);
        int searchTimeout = configListener.getConfigurationHandler()
                .getIntConfig(SystemConfiguration.ES_SEARCH_TIMEOUT);
        int serverPort = configListener.getConfigurationHandler()
                .getIntConfig(SystemConfiguration.ES_PORT);
        List<String> hostList = configListener.getConfigurationHandler()
                .getStringConfigList(SystemConfiguration.ES_HOSTS);

        // create elasticsearch configurations
        ElasticsearchConfiguration elasticsearchConfiguration = new ElasticsearchConfiguration();
        elasticsearchConfiguration.setClusterName(clusterName);
        elasticsearchConfiguration.setTimeout(searchTimeout);
        Map<String, Integer> hosts = new HashMap<>();
        for (String host : hostList) {
            hosts.put(host, serverPort);
        }
        elasticsearchConfiguration.setHostsConfigurations(hosts);
        // create and init executor service
        ElasticsearchExecutorService executorService =
                new ElasticsearchExecutorService(elasticsearchConfiguration);
        executorService.initExecutorService();
        logger.info("Initializing the executor service with {} on {} at {}", hostList, clusterName, serverPort);
        return executorService;
    }

    public ElasticsearchExecutorService getExecutorService() {
        return elasticsearchExecutorService;
    }
}