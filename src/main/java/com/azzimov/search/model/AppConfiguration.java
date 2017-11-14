package com.azzimov.search.model;

/**
 * Created by RahulGupta on 2017-11-09.
 */


import akka.actor.ActorSystem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
//@ComponentScan(basePackages = {"com.azzimov.search.model"})
//@ServletComponentScan(basePackages = {"com.azzimov.search.model"})
public class AppConfiguration {

    @Autowired
    private ApplicationContext applicationContext;

    @Bean
    public ActorSystem actorSystem() {
        ActorSystem system = ActorSystem.create("akka-spring-demo");
        SpringExtension.SPRING_EXTENSION_PROVIDER.get(system).initialize(applicationContext);
        return system;
    }

    /*@Bean
    public SearchModel searchModel(){
        SearchModel searchModel = new SearchModel();
        return searchModel;
    }*/

}
