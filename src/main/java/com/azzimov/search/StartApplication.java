package com.azzimov.search;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.routing.FromConfig;
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
        ActorRef routerFeedback =
                system.actorOf(FromConfig.getInstance().props(
                        SpringExtensionIdProvider.SPRING_EXTENSION_ID_PROVIDER.get(system)
                                .props(FEEDBACK_ACTOR)), FEEDBACK_ACTOR);
        logger.info("Initializing the feedback manager router = {}", routerFeedback);
        applicationActors.put(FEEDBACK_ACTOR, routerFeedback);
    }

    @Autowired
    public void setAppConfiguration(AppConfiguration appConfiguration) {
        this.appConfiguration = appConfiguration;
    }
}
