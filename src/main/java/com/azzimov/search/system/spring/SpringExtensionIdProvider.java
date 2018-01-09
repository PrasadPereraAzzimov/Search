package com.azzimov.search.system.spring;

import akka.actor.AbstractExtensionId;
import akka.actor.ExtendedActorSystem;
import akka.actor.Extension;
import akka.actor.Props;
import org.springframework.context.ApplicationContext;

/**
 * Created by RahulGupta on 2017-11-09.
 * Akka AbstractExtensionId for Azzimov SpringBoot applications.
 */
public class SpringExtensionIdProvider extends AbstractExtensionId<SpringExtensionIdProvider.SpringExtension> {
    public static final SpringExtensionIdProvider SPRING_EXTENSION_ID_PROVIDER = new SpringExtensionIdProvider();

    @Override
    public SpringExtension createExtension(ExtendedActorSystem system) {
        return new SpringExtension();
    }

    /**
     * SpringExtension extends the Extension class that provides the application context to create actors
     */
    public static class SpringExtension implements Extension {
        private volatile ApplicationContext applicationContext;

        void initialize(ApplicationContext applicationContext) {
            this.applicationContext = applicationContext;
        }

        public Props props(String actorBeanName) {
            return Props.create(SpringActorProducer.class, applicationContext, actorBeanName);
        }
    }
}
