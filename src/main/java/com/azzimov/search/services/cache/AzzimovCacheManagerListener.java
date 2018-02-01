package com.azzimov.search.services.cache;

import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

/**
 * Created by prasad on 2/1/18.
 * AzzimovCacheManagerListener is a weblistener that creates and close the azzimov cache manager
 */
@WebListener
public class AzzimovCacheManagerListener implements ServletContextListener {
    AzzimovCacheManager azzimovCacheManager;

    @Override
    public void contextInitialized(ServletContextEvent servletContextEvent) {
        azzimovCacheManager.createAzzimovCacheExecutor();
    }

    @Override
    public void contextDestroyed(ServletContextEvent servletContextEvent) {
        azzimovCacheManager.closeAzzimovCacheExecutor();
    }

    @Autowired
    public void setAzzimovCacheManager(AzzimovCacheManager azzimovCacheManager) {
        this.azzimovCacheManager = azzimovCacheManager;
    }
}
