package com.daveysolutions.gateway;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RateLimiter;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.time.Clock;
import java.time.Duration;

/**
 * Declares the gateway beans used to resolve rate-limit keys and enforce a basic
 * per-client request limit across routed APIs.
 */
@Configuration
class GatewayRateLimitingConfiguration {

    /**
     * Creates the key resolver used by Spring Cloud Gateway rate limiting.
     *
     * @return a key resolver that derives the limit key from the remote client address
     */
    @Bean
    KeyResolver clientAddressKeyResolver() {
        return exchange -> Mono.just(resolveClientAddress(exchange));
    }

    /**
     * Creates the in-memory rate limiter used by gateway routes.
     *
     * @return a fixed-window in-memory rate limiter
     */
    @Bean
    RateLimiter<InMemoryRateLimiterConfig> inMemoryRateLimiter() {
        return new InMemoryRateLimiter(Duration.ofMinutes(1), 1, Clock.systemUTC());
    }

    private static String resolveClientAddress(ServerWebExchange exchange) {
        InetSocketAddress remoteAddress = exchange.getRequest().getRemoteAddress();
        if (remoteAddress == null) {
            return "anonymous";
        }

        if (remoteAddress.getAddress() != null) {
            return remoteAddress.getAddress().getHostAddress();
        }

        return remoteAddress.getHostString();
    }
}
