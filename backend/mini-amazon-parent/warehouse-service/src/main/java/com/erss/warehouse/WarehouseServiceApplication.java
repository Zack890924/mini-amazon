package com.erss.warehouse;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan({"com.erss.warehouse", "com.erss.common"})
public class WarehouseServiceApplication {
    public static void main(String[] args){

        SpringApplication.run(WarehouseServiceApplication.class, args);
    }
}
