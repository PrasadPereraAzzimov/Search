package com.azzimov.search.model;

import akka.actor.UntypedActor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Created by RahulGupta on 2017-11-09.
 */
@Component
@Scope("prototype")
public class GreetingActor extends UntypedActor {
    @Autowired
    private GreetingService greetingService;

    @Override
    public void onReceive(Object message) throws Throwable {
        if(message instanceof Greet){
            String name = ((Greet)message).getName();
            //getSender().tell(greetingService.greet(name), getSelf());
            System.out.println("========================================");
            System.out.println("Heloooo " + name);
            System.out.println("========================================");
        }


    }

    public GreetingActor(){}

    public static class Greet{
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
