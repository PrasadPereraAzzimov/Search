package com.azzimov.search.model;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;

/**
 * Created by RahulGupta on 2017-11-09.
 */
public class Main {
    public static void main(String[] args){
        System.out.println("from mainnnnn........");

        final ActorSystem actorSystem = ActorSystem.create();

        final ActorRef greetingActor = actorSystem.actorOf(
                Props.create(GreetingActor.class)
        );

        GreetingActor.Greet greet = new GreetingActor.Greet();
        greet.setName("Rahul");

        greetingActor.tell(greet, greetingActor);
    }
}
