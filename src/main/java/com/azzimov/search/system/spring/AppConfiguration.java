package com.azzimov.search.system.spring;

import akka.actor.ActorSystem;
import com.azzimov.search.listeners.ConfigListener;
import com.azzimov.search.services.search.executors.SearchExecutorService;
import com.azzimov.search.system.actors.FeedbackManagerActor;
import com.azzimov.search.system.actors.SearchManagerActor;
import com.typesafe.config.Config;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import java.util.Map;

/**
 * Created by RahulGupta on 2017-11-09.
 * Application configurations for Initializing Akka Actor system for Azzimov Search Platform
 */
@Component
@Configuration
public class AppConfiguration {
    private static final String AZZIMOV_SEARCH_ACTOR_SYSTEM = "azzimov-search-actor-system";
    private ApplicationContext applicationContext;
    private ConfigListener configListener;
    private SearchExecutorService searchExecutorService;

    @Autowired
    public AppConfiguration(ApplicationContext applicationContext,
                            ConfigListener configListener,
                            SearchExecutorService searchExecutorService) {
        this.applicationContext = applicationContext;
        this.configListener = configListener;
        this.searchExecutorService = searchExecutorService;
    }

    /**
     * System Actor identifiers
     */
    public static final String FEEDBACK_ACTOR = "router_feedback";
    public static final String SEARCH_ACTOR = "router_search";

    @Bean
    public ActorSystem actorSystem() {
        Map<String, Object> actorSystemConfigs = configListener.getConfigurationHandler()
                .getAkkaActorSystemConfiguration().getAkkaActorSystemConfigurationMap();
        Object configurations = actorSystemConfigs.values().iterator().next();
        ActorSystem system = ActorSystem.create(AZZIMOV_SEARCH_ACTOR_SYSTEM,
                (Config) configurations);
        SpringExtensionIdProvider.SPRING_EXTENSION_ID_PROVIDER.get(system).initialize(applicationContext);
        return system;
    }

    @Bean(name = FEEDBACK_ACTOR)
    public FeedbackManagerActor createFeedbackManagerActor(){
        return new FeedbackManagerActor(searchExecutorService);
    }

    @Bean(name = SEARCH_ACTOR)
    public SearchManagerActor createSearchManagerActor(){
        return new SearchManagerActor(searchExecutorService, configListener, this);
    }

    @Bean(name = "CONFIG_LISTENER")
    public ConfigListener configListener(){
        return new ConfigListener();
    }

    @Bean(name = "SEARCH_EXECUTOR_SERVICE")
    public SearchExecutorService searchExecutorService(){
        return new SearchExecutorService(configListener);
    }
}
