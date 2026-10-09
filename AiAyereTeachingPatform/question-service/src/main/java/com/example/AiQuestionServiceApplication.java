package com.example;

import com.example.config.AiProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(AiProperties.class)
public class AiQuestionServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AiQuestionServiceApplication.class, args);
    }
}