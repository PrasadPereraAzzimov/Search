package com.azzimov.search.system.spring;

import akka.actor.Actor;
import akka.actor.IndirectActorProducer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

/**
 * Created by RahulGupta on 2017-11-09.
 * SpringActorProducer provides Spring extention of using Akka Actors with Azzimov Spring Applicaions
 */
class SpringActorProducer implements IndirectActorProducer {
    private ApplicationContext applicationContext;
    private String beanActorName;

    public SpringActorProducer(ApplicationContext applicationContext, String beanActorName) {
        this.applicationContext = applicationContext;
        this.beanActorName = beanActorName;
    }

    @Override
    public Actor produce() {
        return (Actor) applicationContext.getBean(beanActorName);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Class<? extends Actor> actorClass() {
        return (Class<? extends Actor>) applicationContext.getType(beanActorName);
    }
}
