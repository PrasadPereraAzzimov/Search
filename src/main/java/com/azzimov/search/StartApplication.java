package com.azzimov.search;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.routing.FromConfig;
import akka.routing.RoundRobinPool;
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
import java.util.HashMap;
import java.util.Map;
import static com.azzimov.search.system.spring.AppConfiguration.FEEDBACK_ACTOR;
import static com.azzimov.search.system.spring.AppConfiguration.SEARCH_ACTOR;

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

    public static void main(String[] args){
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
       /* ActorRef routerFeedback =
                system.actorOf(FromConfig.getInstance().props(
                        SpringExtensionIdProvider.SPRING_EXTENSION_ID_PROVIDER.get(system)
                                .props(FEEDBACK_ACTOR)), FEEDBACK_ACTOR);
*/
       ActorRef routerFeedback = system.actorOf(SpringExtensionIdProvider.SPRING_EXTENSION_ID_PROVIDER.get(system)
                .props(FEEDBACK_ACTOR), FEEDBACK_ACTOR);
        logger.info("Initializing the feedback manager router = {}", routerFeedback);
        applicationActors.put(FEEDBACK_ACTOR, routerFeedback);
       /* ActorRef routerSearch =
                system.actorOf(FromConfig.getInstance().props(
                        SpringExtensionIdProvider.SPRING_EXTENSION_ID_PROVIDER.get(system)
                                .props(SEARCH_ACTOR)), SEARCH_ACTOR);


  */     ActorRef routerSearch = system.actorOf(SpringExtensionIdProvider.SPRING_EXTENSION_ID_PROVIDER.get(system)
                .props(SEARCH_ACTOR), SEARCH_ACTOR);
        logger.info("Initializing the search manager router = {}", routerSearch);
        applicationActors.put(SEARCH_ACTOR, routerSearch);
    }

    @Autowired
    public void setAppConfiguration(AppConfiguration appConfiguration) {
        this.appConfiguration = appConfiguration;
    }
}
