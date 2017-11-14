package com.azzimov.search.model;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import java.io.File;
import java.net.URL;

/**
 * Created by RahulGupta on 2017-11-07.
 */
//@Component
@WebListener
public class SearchModel implements ServletContextListener {
    @Override
    public void contextInitialized(ServletContextEvent event) {
//        super.contextInitialized(event);
        System.out.print("=================  Context Initialized =================");
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        URL url1 = classLoader.getResource("application.conf");
        String path1 = url1.getPath();

        URL url = classLoader.getResource("accent");
        String path = url.getPath();
        File[] files = new File(path).listFiles();
        for(File file : files){
            System.out.println(file);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
//        super.contextDestroyed(event);
        System.out.print("=================  Context destroyed =================");
    }



    /*implements ServletContextListener {
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        System.out.println("================  from context listener ===============");
        readAllConfig();
    }

    public void readAllConfig() {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        URL url1 = classLoader.getResource("application.conf");
        String path1 = url1.getPath();

        URL url = classLoader.getResource("corbeil");
        String path = url.getPath();
        File[] files = new File(path).listFiles();
        for(File file : files){
            System.out.println(file);
        }

    }*/
}
