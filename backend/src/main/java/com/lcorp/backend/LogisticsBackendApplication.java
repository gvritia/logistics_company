package com.lcorp.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.context.annotation.CommonAnnotationBeanPostProcessor;

@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class LogisticsBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(LogisticsBackendApplication.class, args);



    }

}
