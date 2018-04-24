package com.azzimov.search;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.routing.FromConfig;
import com.azzimov.search.services.cache.AzzimovCacheManager;
import com.azzimov.search.services.search.learn.LearnCentroidCluster;
import com.azzimov.search.services.search.learn.LearnStatModelService;
import com.azzimov.search.system.spring.AppConfiguration;
import com.azzimov.search.system.spring.SpringExtensionIdProvider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import static com.azzimov.search.system.spring.AppConfiguration.AGGREGATE_ACTOR;
import static com.azzimov.search.system.spring.AppConfiguration.FEEDBACK_ACTOR;
import static com.azzimov.search.system.spring.AppConfiguration.SEARCH_ACTOR;
import static com.azzimov.search.system.spring.AppConfiguration.SESSION_LEARN_ACTOR;
import static com.azzimov.search.system.spring.AppConfiguration.SUGGEST_AUTOCOMPLETE_ACTOR;

/**
 * Created by RahulGupta on 2017-12-21.
 * Azzimov Search Application Starter
 */
@SpringBootApplication
@EnableAutoConfiguration
@ServletComponentScan
public class StartApplication {
    private AppConfiguration appConfiguration;
    private static final Logger logger = LogManager.getLogger(StartApplication.class);
    private ActorSystem system = null;
    private Map<String, ActorRef> applicationActors = null;
    private AzzimovCacheManager azzimovCacheManager;
    private LearnStatModelService learnStatModelService;

    public static void main(String[] args) {
        SpringApplication.run(StartApplication.class, args);
        logger.info("Application has been started..........");
    }

    /**
     * The init for application related services like Akka Actors that provide different system components
     * for Azzimov Search
     */
    @PostConstruct
    public void init() {
        applicationActors = new HashMap<>();
        system = appConfiguration.actorSystem();
        ActorRef routerFeedback =
                system.actorOf(FromConfig.getInstance().props(
                        SpringExtensionIdProvider.SPRING_EXTENSION_ID_PROVIDER.get(system)
                                .props(FEEDBACK_ACTOR)), FEEDBACK_ACTOR);

        logger.info("Initializing the feedback manager router = {}", routerFeedback);
        applicationActors.put(FEEDBACK_ACTOR, routerFeedback);
        ActorRef routerSearch =
                system.actorOf(FromConfig.getInstance().props(
                        SpringExtensionIdProvider.SPRING_EXTENSION_ID_PROVIDER.get(system)
                                .props(SEARCH_ACTOR)), SEARCH_ACTOR);

        logger.info("Initializing the search manager router = {}", routerSearch);
        applicationActors.put(SEARCH_ACTOR, routerSearch);

        ActorRef routerAggregation =
                system.actorOf(FromConfig.getInstance().props(
                        SpringExtensionIdProvider.SPRING_EXTENSION_ID_PROVIDER.get(system)
                                .props(AGGREGATE_ACTOR)), AGGREGATE_ACTOR);

        logger.info("Initializing the aggregate manager router = {}", routerAggregation);
        applicationActors.put(AGGREGATE_ACTOR, routerSearch);

        ActorRef routerSessionLearn =
                system.actorOf(FromConfig.getInstance().props(
                        SpringExtensionIdProvider.SPRING_EXTENSION_ID_PROVIDER.get(system)
                                .props(SESSION_LEARN_ACTOR)), SESSION_LEARN_ACTOR);
        logger.info("Initializing the session learn manager router = {}", routerAggregation);
        applicationActors.put(SESSION_LEARN_ACTOR, routerSessionLearn);

        ActorRef routersuggestionAutocompletion =
                system.actorOf(FromConfig.getInstance().props(
                        SpringExtensionIdProvider.SPRING_EXTENSION_ID_PROVIDER.get(system)
                                .props(SUGGEST_AUTOCOMPLETE_ACTOR)), SUGGEST_AUTOCOMPLETE_ACTOR);
        logger.info("Initializing the session learn manager router = {}", routersuggestionAutocompletion);
        applicationActors.put(SUGGEST_AUTOCOMPLETE_ACTOR, routersuggestionAutocompletion);
        try {
            logger.info("Retrieving learn centroids = {}", LearnCentroidCluster.CENTROID_GUIDANCE_KEY);
            this.learnStatModelService.updateGuidanceLearningModelManager();
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

    @Autowired
    public void setAppConfiguration(AppConfiguration appConfiguration) {
        this.appConfiguration = appConfiguration;
    }

    @Autowired
    public void setAzzimovCacheManager(AzzimovCacheManager azzimovCacheManager) {
        this.azzimovCacheManager = azzimovCacheManager;
    }

    @Autowired
    public void setLearnStatModelService(LearnStatModelService learnStatModelService) {
        this.learnStatModelService = learnStatModelService;
    }
}
