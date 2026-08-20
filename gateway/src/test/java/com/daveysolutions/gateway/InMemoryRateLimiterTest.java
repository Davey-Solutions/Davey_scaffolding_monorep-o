package com.daveysolutions.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.ratelimit.RateLimiter;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryRateLimiterTest {

    @Test
    void returnsRateLimitHeadersForAllowedAndRejectedRequests() {
        InMemoryRateLimiter rateLimiter = new InMemoryRateLimiter(
                Duration.ofMinutes(1),
                1,
                Clock.fixed(Instant.parse("2026-08-19T00:00:00Z"), ZoneOffset.UTC)
        );

        RateLimiter.Response firstResponse = rateLimiter.isAllowed("auth-service", "127.0.0.1").block();
        RateLimiter.Response secondResponse = rateLimiter.isAllowed("auth-service", "127.0.0.1").block();

        assertThat(firstResponse).isNotNull();
        assertThat(firstResponse.isAllowed()).isTrue();
        assertThat(firstResponse.getHeaders())
                .containsEntry("X-RateLimit-Limit", "1")
                .containsEntry("X-RateLimit-Remaining", "0");

        assertThat(secondResponse).isNotNull();
        assertThat(secondResponse.isAllowed()).isFalse();
        assertThat(secondResponse.getHeaders())
                .containsEntry("X-RateLimit-Limit", "1")
                .containsEntry("X-RateLimit-Remaining", "0");
    }
}
