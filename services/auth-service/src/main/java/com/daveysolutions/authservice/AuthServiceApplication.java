package com.daveysolutions.authservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the Auth Service Spring Boot application.
 *
 * <p>This service hosts authentication and user management capabilities for the Davey scaffolding
 * platform.
 */
@SpringBootApplication
public class AuthServiceApplication {

    /**
     * Starts the Auth Service application.
     *
     * @param args command-line arguments passed to the Spring application context
     */
    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }
}
