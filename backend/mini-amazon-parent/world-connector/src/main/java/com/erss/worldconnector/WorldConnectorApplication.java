package com.erss.worldconnector;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;


@SpringBootApplication
@ComponentScan(basePackages = {"com.erss.worldconnector", "com.erss.common"})
public class WorldConnectorApplication {
    public static void main(String[] args){
        SpringApplication.run(WorldConnectorApplication.class, args);
    }
}
