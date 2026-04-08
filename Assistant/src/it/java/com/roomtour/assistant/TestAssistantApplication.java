package com.roomtour.assistant;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Bootstrap for integration tests only.
 * Provides the @SpringBootConfiguration that @SpringBootTest needs to wire
 * the full assistant context without a production main class.
 */
@SpringBootApplication
public class TestAssistantApplication {
    public static void main(String[] args) {
        SpringApplication.run(TestAssistantApplication.class, args);
    }
}
