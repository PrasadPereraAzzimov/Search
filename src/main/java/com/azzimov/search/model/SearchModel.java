package com.azzimov.search.model;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Created by RahulGupta on 2017-11-07.
 */
@WebListener
public class SearchModel implements ServletContextListener {

    @Autowired
    private ApplicationContext applicationContext;

    @Override
    public void contextInitialized(ServletContextEvent event){
        try{

            Resource[] resources = applicationContext.getResources("classpath*:**/resources/*.conf");
            for(Resource resource : resources){
                InputStreamReader inputStreamReader = new InputStreamReader(resource.getInputStream());
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                StringBuilder stringBuilder = new StringBuilder();
                String read = null;
                while ((read = bufferedReader.readLine()) != null){
                    stringBuilder.append(read);
                    stringBuilder.append("\n");
                }
                System.out.println("====================================");
                System.out.println(stringBuilder.toString());
                System.out.println("====================================");
            }
        }
        catch (IOException e){
            e.printStackTrace();
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        System.out.print("=================  Context destroyed =================");
    }
}
