package com.azzimov.search.listeners;

import com.azzimov.search.common.util.config.ConfigurationHandler;
import com.azzimov.search.common.util.config.SystemConfiguration;
import com.azzimov.search.common.util.config.types.AkkaActorSystemConfiguration;
import com.azzimov.search.common.util.config.types.AnalyticsTypeConfiguration;
import com.azzimov.search.common.util.config.types.AutocompleteTypeConfiguration;
import com.azzimov.search.common.util.config.types.RecommendationTypeConfiguration;
import com.azzimov.search.common.util.config.types.SearchTypeConfiguration;
import com.azzimov.search.common.util.config.types.SystemTypeConfiguration;
import com.azzimov.search.common.util.config.types.TaskTypeConfiguration;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValue;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by RahulGupta on 2017-12-21.
 * ConfigListener Reads the config files and load them
 */
@WebListener
public class ConfigListener implements ServletContextListener {
    private ConfigurationHandler configurationHandler = ConfigurationHandler.getInstance();
    private static final Logger logger = LogManager.getLogger(ConfigListener.class);
    private static final String SYSTEM_CONFIGURATION_PATH = "configurations/system.conf";
    private static final String SEARCH_CONFIGURATION_PATH = "configurations/search.conf";
    private static final String RECOMMENDATION_CONFIGURATION_PATH = "configurations/recommendation.conf";
    private static final String TASK_CONFIGURATION_PATH = "configurations/task.conf";
    private static final String AUTOCOMPLETE_CONFIGURATION_PATH = "configurations/autocomplete.conf";
    private static final String ANALYTICS_CONFIGURATION_PATH = "configurations/analytics.conf";
    private static final String SYSTEM_AKKA_CONFIGURATION_PATH = "configurations/akka_configurations.conf";

    private Map<String, Object> analyticsConfigMap;
    private Map<String, Object> autocompleteConfigMap;
    private Map<String, Object> recommendationConfigMap;
    private Map<String, Object> searchConfigMap;
    private Map<String, Object> systemConfigMap;
    private Map<String, Object> taskConfigMap;
    private Map<String, Object> systemAkkConfigMap;

    @Override
    public void contextInitialized(ServletContextEvent servletContextEvent) {
        String clientName;
        try {
            analyticsConfigMap = new HashMap<>();
            autocompleteConfigMap = new HashMap<>();
            recommendationConfigMap = new HashMap<>();
            searchConfigMap = new HashMap<>();
            systemConfigMap = new HashMap<>();
            taskConfigMap = new HashMap<>();
            systemAkkConfigMap = new HashMap<>();

            System.out.println("========= Config Listener has been started ===========");

            //read global configuration
            readConfig(SEARCH_CONFIGURATION_PATH, searchConfigMap, true);
            readConfig(SYSTEM_CONFIGURATION_PATH, systemConfigMap, true);
            readConfig(RECOMMENDATION_CONFIGURATION_PATH, recommendationConfigMap, true);
            readConfig(TASK_CONFIGURATION_PATH, taskConfigMap, true);
            readConfig(AUTOCOMPLETE_CONFIGURATION_PATH, autocompleteConfigMap, true);
            readConfig(ANALYTICS_CONFIGURATION_PATH, analyticsConfigMap, true);
            readConfig(SYSTEM_AKKA_CONFIGURATION_PATH, systemAkkConfigMap, false);

            clientName = !systemConfigMap.containsKey(SystemConfiguration.CLIENT_NAME) ? null :
                    configurationHandler.getStringConfig(SystemConfiguration.CLIENT_NAME);

            //read client specific configuration
            if (StringUtils.isNotBlank(clientName)) {
                readConfig(clientName + "/" + SEARCH_CONFIGURATION_PATH, searchConfigMap, true);
                readConfig(clientName + "/" + SYSTEM_CONFIGURATION_PATH, systemConfigMap, true);
                readConfig(clientName + "/" + RECOMMENDATION_CONFIGURATION_PATH, recommendationConfigMap, true);
                readConfig(clientName + "/" + TASK_CONFIGURATION_PATH, taskConfigMap, true);
                readConfig(clientName + "/" + AUTOCOMPLETE_CONFIGURATION_PATH, autocompleteConfigMap, true);
                readConfig(clientName + "/" + ANALYTICS_CONFIGURATION_PATH, analyticsConfigMap, true);
            }

            AnalyticsTypeConfiguration analyticsTypeConfiguration = new AnalyticsTypeConfiguration(analyticsConfigMap);
            configurationHandler.setAnalyticsTypeConfiguration(analyticsTypeConfiguration);

            AutocompleteTypeConfiguration autocompleteTypeConfiguration = new AutocompleteTypeConfiguration(autocompleteConfigMap);
            configurationHandler.setAutocompleteTypeConfiguration(autocompleteTypeConfiguration);

            RecommendationTypeConfiguration recommendationTypeConfiguration = new RecommendationTypeConfiguration(recommendationConfigMap);
            configurationHandler.setRecommendationTypeConfiguration(recommendationTypeConfiguration);

            SystemTypeConfiguration systemTypeConfiguration = new SystemTypeConfiguration(systemConfigMap);
            configurationHandler.setSystemTypeConfiguration(systemTypeConfiguration);

            SearchTypeConfiguration searchTypeConfiguration = new SearchTypeConfiguration(searchConfigMap);
            configurationHandler.setSearchTypeConfiguration(searchTypeConfiguration);

            TaskTypeConfiguration taskTypeConfiguration = new TaskTypeConfiguration(taskConfigMap);
            configurationHandler.setTaskTypeConfiguration(taskTypeConfiguration);

            AkkaActorSystemConfiguration akkaActorSystemConfiguration = new AkkaActorSystemConfiguration(systemAkkConfigMap);
            configurationHandler.setAkkaActorSystemConfiguration(akkaActorSystemConfiguration);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void readConfig(String path, Map<String, Object> configMap, boolean populateData) {
        InputStream inputStream = null;
        InputStreamReader inputStreamReader = null;
        BufferedReader bufferedReader = null;
        try {
            inputStream = this.getClass().getClassLoader().getResourceAsStream(path);
            inputStreamReader = new InputStreamReader(inputStream);
            if (populateData) {
                bufferedReader = new BufferedReader(inputStreamReader);
                Config config = ConfigFactory.parseReader(inputStreamReader);
                for (Map.Entry<String, ConfigValue> entry : config.entrySet()) {
                    configMap.put(entry.getKey(), entry.getValue().unwrapped());
                }
            } else {
                Config config = ConfigFactory.parseReader(inputStreamReader);
                String configKey = config.root().keySet().iterator().next();
                configMap.put(configKey, config);
            }
        } catch (Exception exception) {
            logger.error("Error occurred while reading the configurations {}", exception);
        } finally {
            try {
                if (bufferedReader != null) {
                    bufferedReader.close();
                }
                if (inputStreamReader != null) {
                    inputStreamReader.close();
                }
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (IOException ioException) {
                logger.error("Error occurred while reading the configurations {}", ioException);
            }
        }
    }

    public ConfigurationHandler getConfigurationHandler() {
        return configurationHandler;
    }

    @Override
    public void contextDestroyed(ServletContextEvent servletContextEvent) {
        System.out.println("========= Config Listener has been destroyed ===========");
    }

    public static Map<String, String> retrieveTargetRepositoriesforDocuments(List<Object> targetRepositoryConfigs,
                                                                             ConfigurationHandler configurationHandler) {
        Map<String, String> targetRepositories = new HashMap<>();

        for (Object object : targetRepositoryConfigs) {
            Map<String, String> targetCongigs = (Map<String, String>) object;
            targetRepositories.putAll(targetCongigs);
        }
        // Map index names from system configs to search configs
        List<String> docRepositories = new ArrayList<>();
        if (configurationHandler.getStringConfig(SystemConfiguration.ES_ECOM_INDEX) != null)
            docRepositories.add(configurationHandler.getStringConfig(SystemConfiguration.ES_ECOM_INDEX));
        if (configurationHandler.getStringConfig(SystemConfiguration.ES_DIGITAL_INDEX) != null)
            docRepositories.add(configurationHandler.getStringConfig(SystemConfiguration.ES_DIGITAL_INDEX));
        if (configurationHandler.getStringConfig(SystemConfiguration.ES_ASSOCIATION_INDEX) != null)
            docRepositories.add(configurationHandler.getStringConfig(SystemConfiguration.ES_ASSOCIATION_INDEX));
        if (configurationHandler.getStringConfig(SystemConfiguration.ES_AGGREGATED_FEEDBACK) != null)
            docRepositories.add(configurationHandler.getStringConfig(SystemConfiguration.ES_AGGREGATED_FEEDBACK));
        if (configurationHandler.getStringConfig(SystemConfiguration.ES_FEEDBACK_INDEX) != null)
            docRepositories.add(configurationHandler.getStringConfig(SystemConfiguration.ES_FEEDBACK_INDEX));
        if (configurationHandler.getStringConfig(SystemConfiguration.ES_SYNONYM_INDEX) != null)
            docRepositories.add(configurationHandler.getStringConfig(SystemConfiguration.ES_SYNONYM_INDEX));
        Map<String, String> targetRepositoriesFinal = new HashMap<>();
        for (String repo : docRepositories) {
            for (Map.Entry<String, String> entry : targetRepositories.entrySet()) {
                if (repo.equals(entry.getValue()) || repo.startsWith(entry.getValue())) {
                        targetRepositoriesFinal.put(entry.getKey(), repo);
                }
            }
        }
        return targetRepositoriesFinal;
    }
}

