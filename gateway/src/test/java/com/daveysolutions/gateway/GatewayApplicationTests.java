package com.daveysolutions.gateway;

import io.netty.handler.codec.http.HttpResponseStatus;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
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
                    (request, response) -> response.status(HttpResponseStatus.OK)
                            .sendString(Mono.just("ok"))))
            .bindNow();

    @Autowired
    private WebTestClient webTestClient;

    @DynamicPropertySource
    static void configureRouteUris(DynamicPropertyRegistry registry) {
        registry.add("JOB_SERVICE_URI", () -> "http://127.0.0.1:" + DOWNSTREAM_SERVER.port());
        registry.add("AUTH_SERVICE_URI", () -> "http://127.0.0.1:" + DOWNSTREAM_SERVER.port());
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
}
