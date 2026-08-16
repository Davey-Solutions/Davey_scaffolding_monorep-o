package com.daveysolutions.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the Gateway Spring Boot application.
 *
 * <p>This module hosts the Spring Cloud Gateway entrypoint for the Davey scaffolding
 * platform.
 */
@SpringBootApplication
public class GatewayApplication {

    /**
     * Starts the Gateway application.
     *
     * @param args command-line arguments passed to the Spring application context
     */
    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
