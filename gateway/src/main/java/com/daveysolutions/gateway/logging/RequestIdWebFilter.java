package com.daveysolutions.gateway.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Ensures every gateway request carries a request ID header and emits a gateway log entry with it.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdWebFilter implements WebFilter {

    public static final String REQUEST_ID_HEADER = "X-Request-Id";

    private static final Logger log = LoggerFactory.getLogger(RequestIdWebFilter.class);

    /**
     * Adds or propagates a request ID header for the current exchange.
     *
     * @param exchange the current reactive server exchange
     * @param chain the remaining reactive filter chain
     * @return the completion signal for the filtered exchange
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String requestId = resolveRequestId(exchange.getRequest().getHeaders());
        ServerHttpRequest request = exchange.getRequest()
                .mutate()
                .headers(headers -> headers.set(REQUEST_ID_HEADER, requestId))
                .build();
        ServerWebExchange mutatedExchange = exchange.mutate().request(request).build();
        mutatedExchange.getResponse().getHeaders().set(REQUEST_ID_HEADER, requestId);
        return chain.filter(mutatedExchange)
                .doFinally(signalType -> logRequest(mutatedExchange, requestId));
    }

    private static String resolveRequestId(HttpHeaders headers) {
        String requestId = headers.getFirst(REQUEST_ID_HEADER);
        if (requestId == null || requestId.isBlank()) {
            return UUID.randomUUID().toString();
        }
        return requestId;
    }

    private static void logRequest(ServerWebExchange exchange, String requestId) {
        HttpStatusCode statusCode = exchange.getResponse().getStatusCode();
        try (MDC.MDCCloseable ignored = MDC.putCloseable("requestId", requestId)) {
            log.info("Handled {} {} -> {}",
                    exchange.getRequest().getMethod(),
                    exchange.getRequest().getPath().value(),
                    statusCode != null ? statusCode.value() : 0);
        }
    }
}
