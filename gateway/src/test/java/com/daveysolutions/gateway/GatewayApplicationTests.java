package com.daveysolutions.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import io.netty.handler.codec.http.HttpResponseStatus;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.ratelimit.RateLimiter;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewayApplicationTests {

    private static final DisposableServer DOWNSTREAM_SERVER = HttpServer.create()
            .host("127.0.0.1")
            .port(0)
            .route(routes -> routes.route(request -> true,
                    (request, response) -> response.status(
                                    request.uri().endsWith("/api/v1/auth/refresh")
                                            ? HttpResponseStatus.INTERNAL_SERVER_ERROR
                                            : HttpResponseStatus.OK)
                            .sendString(Mono.just("ok"))))
            .bindNow();

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private RateLimiter<InMemoryRateLimiterConfig> rateLimiter;

    @DynamicPropertySource
    static void configureRouteUris(DynamicPropertyRegistry registry) {
        registry.add("JOB_SERVICE_URI", () -> "http://127.0.0.1:" + DOWNSTREAM_SERVER.port());
        registry.add("AUTH_SERVICE_URI", () -> "http://127.0.0.1:" + DOWNSTREAM_SERVER.port());
    }

    @BeforeEach
    void resetRateLimiter() throws ReflectiveOperationException {
        clearRateLimiterState();
    }

    private void clearRateLimiterState() throws ReflectiveOperationException {
        InMemoryRateLimiter inMemoryRateLimiter = (InMemoryRateLimiter) rateLimiter;

        var windowsField = InMemoryRateLimiter.class.getDeclaredField("windows");
        windowsField.setAccessible(true);
        ((Map<?, ?>) windowsField.get(inMemoryRateLimiter)).clear();

        var nextEvictionAtField = InMemoryRateLimiter.class.getDeclaredField("nextEvictionAt");
        nextEvictionAtField.setAccessible(true);
        ((java.util.concurrent.atomic.AtomicLong) nextEvictionAtField.get(inMemoryRateLimiter)).set(0L);
    }

    @AfterAll
    static void stopDownstreamServer() {
        DOWNSTREAM_SERVER.disposeNow();
    }

    @Test
    void contextLoads() {
    }

    @Test
    void healthEndpointReturnsOk() {
        webTestClient.get()
                .uri("/actuator/health")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("UP");
    }

    @Test
    void excessRequestsReceiveTooManyRequests() {
        webTestClient.post()
                .uri("/api/v1/auth/login")
                .exchange()
                .expectStatus().isOk();

        webTestClient.post()
                .uri("/api/v1/auth/login")
                .exchange()
                .expectStatus().isEqualTo(429);
    }

    @Test
    void metricsEndpointReportsRequestSignals() {
        webTestClient.get()
                .uri("/api/v1/auth/login")
                .exchange()
                .expectStatus().isOk();

        try {
            clearRateLimiterState();
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }

        webTestClient.get()
                .uri("/api/v1/auth/refresh")
                .exchange()
                .expectStatus().is5xxServerError();

        final Map<String, Object> body = webTestClient.get()
                .uri("/actuator/metrics/spring.cloud.gateway.requests")
                .exchange()
                .expectStatus().isOk()
                .expectBody(Map.class)
                .returnResult()
                .getResponseBody();

        assertThat(body).containsEntry("name", "spring.cloud.gateway.requests");
        assertThat(extractTagValues(body, "status")).contains("OK", "INTERNAL_SERVER_ERROR");
        assertThat(extractMeasurementStatistics(body)).contains("COUNT", "TOTAL_TIME", "MAX");
    }

    private List<String> extractTagValues(Map<String, Object> body, String tagName) {
        return (List<String>) ((List<Map<String, Object>>) body.get("availableTags")).stream()
                .filter(tag -> tagName.equals(tag.get("tag")))
                .findFirst()
                .orElseThrow()
                .get("values");
    }

    private List<String> extractMeasurementStatistics(Map<String, Object> body) {
        return ((List<Map<String, Object>>) body.get("measurements")).stream()
                .map(measurement -> String.valueOf(measurement.get("statistic")))
                .toList();
    }
}
