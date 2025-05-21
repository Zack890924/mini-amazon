package com.erss.order;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

//@ComponentScan({"com.erss.warehouse", "com.erss.common.security"})
@SpringBootApplication(
        exclude = {
                SecurityAutoConfiguration.class
//                ManagementWebSecurityAutoConfiguration.class
        }
)
public class OrderServiceApplication {
    public static void main(String[] args){

        SpringApplication.run(OrderServiceApplication.class, args);
    }
}