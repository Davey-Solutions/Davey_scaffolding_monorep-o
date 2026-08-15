package com.daveysolutions.jobservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the Job Service Spring Boot application.
 *
 * <p>This service provides job management capabilities within the Davey scaffolding platform.
 * On startup it exposes {@code /actuator/health} for liveness and readiness checks.
 * Domain endpoints will be added in subsequent milestones.
 */
@SpringBootApplication
public class JobServiceApplication {

    /**
     * Starts the Job Service application.
     *
     * @param args command-line arguments passed to the Spring application context
     */
    public static void main(String[] args) {
        SpringApplication.run(JobServiceApplication.class, args);
    }
}
