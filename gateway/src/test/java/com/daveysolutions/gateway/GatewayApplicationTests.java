package com.daveysolutions.gateway;

import com.daveysolutions.gateway.logging.RequestIdWebFilter;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.junit.jupiter.api.extension.ExtendWith;
import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(OutputCaptureExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewayApplicationTests {

    private static final AtomicReference<String> DOWNSTREAM_REQUEST_ID = new AtomicReference<>();

    private static final DisposableServer DOWNSTREAM_SERVER = HttpServer.create()
            .host("127.0.0.1")
            .port(0)
            .route(routes -> routes.route(request -> true,
                    (request, response) -> {
                        DOWNSTREAM_REQUEST_ID.set(request.requestHeaders().get(RequestIdWebFilter.REQUEST_ID_HEADER));
                        return response.status(HttpResponseStatus.OK)
                                .sendString(Mono.just("ok"));
                    }))
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
    void routedRequestsReceiveGeneratedRequestIds(CapturedOutput output) {
        var result = webTestClient.post()
                .uri("/api/v1/auth/login")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().exists(RequestIdWebFilter.REQUEST_ID_HEADER)
                .returnResult(String.class);

        String requestId = result.getResponseHeaders().getFirst(RequestIdWebFilter.REQUEST_ID_HEADER);
        assertThat(requestId).isNotBlank();
        assertThat(DOWNSTREAM_REQUEST_ID.get()).isEqualTo(requestId);
        assertThat(output.getOut()).contains("\"requestId\":\"" + requestId + "\"");
    }

    @Test
    void routedRequestsPropagateExistingRequestIds(CapturedOutput output) {
        String requestId = "gateway-test-request-id";
        webTestClient.post()
                .uri("/api/v1/auth/login")
                .header(RequestIdWebFilter.REQUEST_ID_HEADER, requestId)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals(RequestIdWebFilter.REQUEST_ID_HEADER, requestId);

        assertThat(DOWNSTREAM_REQUEST_ID.get()).isEqualTo(requestId);
        assertThat(output.getOut()).contains("\"requestId\":\"" + requestId + "\"");
    }
}
