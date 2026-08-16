package com.daveysolutions.gateway;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests that verify the route predicates declared in {@code application.yml}
 * are loaded correctly by Spring Cloud Gateway.
 *
 * <p>The full application context is started with a random port; no downstream services
 * need to be running — we only assert that the route definitions (id, URI, and path
 * predicate) are present in the {@link RouteLocator}.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RoutePredicateTest {

    @Autowired
    private RouteLocator routeLocator;

    /** All routes resolved from the {@link RouteLocator}, populated before each test. */
    private List<Route> routes;

    /** Resolves all gateway routes once before each test method. */
    @BeforeEach
    void loadRoutes() {
        routes = routeLocator.getRoutes().collectList().block();
    }

    /**
     * Asserts that a route with id {@code job-service} exists, targets
     * {@code http://job-service:8080}, and carries a path predicate for
     * {@code /api/v1/jobs/**}.
     */
    @Test
    void jobServiceRouteIsConfigured() {
        assertThat(routes)
                .isNotNull()
                .anySatisfy(route -> {
                    assertThat(route.getId()).isEqualTo("job-service");
                    assertThat(route.getUri().toString()).isEqualTo("http://job-service:8080");
                    assertThat(route.getPredicate().toString()).contains("/api/v1/jobs");
                });
    }

    /**
     * Asserts that a route with id {@code auth-service} exists, targets
     * {@code http://auth-service:8080}, and carries a path predicate for
     * {@code /api/v1/auth/**}.
     */
    @Test
    void authServiceRouteIsConfigured() {
        assertThat(routes)
                .isNotNull()
                .anySatisfy(route -> {
                    assertThat(route.getId()).isEqualTo("auth-service");
                    assertThat(route.getUri().toString()).isEqualTo("http://auth-service:8080");
                    assertThat(route.getPredicate().toString()).contains("/api/v1/auth");
                });
    }
}
