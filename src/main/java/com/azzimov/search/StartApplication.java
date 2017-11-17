package com.azzimov.search;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import com.azzimov.search.model.AppConfiguration;
import com.azzimov.search.model.GreetingActor;
import com.azzimov.search.model.SpringExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

import javax.annotation.PostConstruct;


/**
 * Created by RahulGupta on 2017-11-08.
 */

@SpringBootApplication
@EnableAutoConfiguration
@ComponentScan
@ServletComponentScan
@EnableScheduling
public class StartApplication {

    @Autowired
    private AppConfiguration appConfiguration;

    @Autowired
    private ApplicationContext applicationContext;

    private ActorSystem system = null;

    public static void main(String[] args){
        SpringApplication.run(StartApplication.class, args);

    }

    @PostConstruct
    public void init(){
        system = appConfiguration.actorSystem();

        ActorRef greetingActor = system.actorOf(SpringExtension.SPRING_EXTENSION_PROVIDER.get(system)
                .props("greetingActor"));

        GreetingActor.Greet greet = new GreetingActor.Greet();
        greet.setName("Rahul");

        greetingActor.tell(greet, greetingActor);
    }

}
