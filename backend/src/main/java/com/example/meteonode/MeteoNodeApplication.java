package com.example.meteonode;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MeteoNodeApplication {

    public static void main(String[] args) {
        SpringApplication.run(MeteoNodeApplication.class, args);
    }

}
