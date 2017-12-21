package com.azzimov.search.listeners;

import com.azzimov.search.common.util.config.ConfigurationHandler;
import com.azzimov.search.common.util.config.SystemConfiguration;
import com.azzimov.search.common.util.config.types.AnalyticsTypeConfiguration;
import com.azzimov.search.common.util.config.types.AutocompleteTypeConfiguration;
import com.azzimov.search.common.util.config.types.RecommendationTypeConfiguration;
import com.azzimov.search.common.util.config.types.SearchTypeConfiguration;
import com.azzimov.search.common.util.config.types.SystemTypeConfiguration;
import com.azzimov.search.common.util.config.types.TaskTypeConfiguration;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by RahulGupta on 2017-12-21.
 */
@WebListener
public class ConfigListener implements ServletContextListener {

    private ConfigurationHandler configurationHandler = ConfigurationHandler.getInstance();

    private static final String SYSTEM_CONFIGURATION_PATH = "configurations/system.conf";
    private static final String SEARCH_CONFIGURATION_PATH = "configurations/search.conf";
    private static final String RECOMMENDATION_CONFIGURATION_PATH = "configurations/recommendation.conf";
    private static final String TASK_CONFIGURATION_PATH = "configurations/task.conf";
    private static final String AUTOCOMPLETE_CONFIGURATION_PATH = "configurations/autocomplete.conf";
    private static final String ANALYTICS_CONFIGURATION_PATH = "configurations/analytics.conf";

    private Map<String, Object> analyticsConfigMap;
    private Map<String, Object> autocompleteConfigMap;
    private Map<String, Object> recommendationConfigMap;
    private Map<String, Object> searchConfigMap;
    private Map<String, Object> systemConfigMap;
    private Map<String, Object> taskConfigMap;

    @Override
    public void contextInitialized(ServletContextEvent servletContextEvent) {
        String clientName = null;
        try{
            analyticsConfigMap = new HashMap<>();
            autocompleteConfigMap = new HashMap<>();
            recommendationConfigMap = new HashMap<>();
            searchConfigMap = new HashMap<>();
            systemConfigMap = new HashMap<>();
            taskConfigMap = new HashMap<>();

            System.out.println("========= Config Listener has been started ===========");

            //read global configuration
            readConfig(SEARCH_CONFIGURATION_PATH, searchConfigMap);
            readConfig(SYSTEM_CONFIGURATION_PATH, systemConfigMap);
            readConfig(RECOMMENDATION_CONFIGURATION_PATH, recommendationConfigMap);
            readConfig(TASK_CONFIGURATION_PATH, taskConfigMap);
            readConfig(AUTOCOMPLETE_CONFIGURATION_PATH, autocompleteConfigMap);
            readConfig(ANALYTICS_CONFIGURATION_PATH, analyticsConfigMap);

            clientName = !systemConfigMap.containsKey(SystemConfiguration.CLIENT_NAME) ? null :
                    configurationHandler.getStringConfig(SystemConfiguration.CLIENT_NAME);

            //read client specific configuration
            if(StringUtils.isNotBlank(clientName)){
                readConfig(clientName + "/" + SEARCH_CONFIGURATION_PATH, searchConfigMap);
                readConfig(clientName + "/" + SYSTEM_CONFIGURATION_PATH, systemConfigMap);
                readConfig(clientName + "/" + RECOMMENDATION_CONFIGURATION_PATH, recommendationConfigMap);
                readConfig(clientName + "/" + TASK_CONFIGURATION_PATH, taskConfigMap);
                readConfig(clientName + "/" + AUTOCOMPLETE_CONFIGURATION_PATH, autocompleteConfigMap);
                readConfig(clientName + "/" + ANALYTICS_CONFIGURATION_PATH, analyticsConfigMap);
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
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }


    private void readConfig(String path, Map<String, Object> configMap){
        InputStream inputStream = null;
        InputStreamReader inputStreamReader = null;
        BufferedReader bufferedReader = null;
        String readLine = null;
        try{
            inputStream = this.getClass().getClassLoader().getResourceAsStream(path);
            inputStreamReader = new InputStreamReader(inputStream);
            bufferedReader = new BufferedReader(inputStreamReader);
            while ((readLine = bufferedReader.readLine()) != null){
                String[] configs = readLine.split("=");
                String configKey = configs[0].trim();
                String configValue = configs[1].trim();
                configMap.put(configKey, configValue);
            }
        }
        catch (IOException ioException){

        }
        finally {
            try{
                if(bufferedReader != null){
                    bufferedReader.close();
                }

                if(inputStreamReader != null){
                    inputStreamReader.close();
                }

                if(inputStream != null){
                    inputStream.close();
                }
            }
            catch (IOException ioException){

            }


        }


    }

    @Override
    public void contextDestroyed(ServletContextEvent servletContextEvent) {
        System.out.println("========= Config Listener has been destroyed ===========");
    }
}

