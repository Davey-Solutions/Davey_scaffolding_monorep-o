package com.daveysolutions.gateway;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RateLimiter;
import org.springframework.cloud.gateway.support.AbstractStatefulConfigurable;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

@Configuration
class GatewayRateLimitingConfiguration {

    @Bean
    KeyResolver clientAddressKeyResolver() {
        return exchange -> Mono.just(resolveClientAddress(exchange));
    }

    @Bean
    RateLimiter<InMemoryRateLimiter.Config> inMemoryRateLimiter() {
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

    static final class InMemoryRateLimiter extends AbstractStatefulConfigurable<InMemoryRateLimiter.Config>
            implements RateLimiter<InMemoryRateLimiter.Config> {

        private static final String REMAINING_HEADER = "X-RateLimit-Remaining";
        private static final String LIMIT_HEADER = "X-RateLimit-Limit";

        private final Duration replenishPeriod;
        private final int burstCapacity;
        private final Clock clock;
        private final ConcurrentMap<String, Window> windows = new ConcurrentHashMap<>();
        private final AtomicLong nextEvictionAt = new AtomicLong();

        InMemoryRateLimiter(Duration replenishPeriod, int burstCapacity, Clock clock) {
            super(Config.class);
            this.replenishPeriod = replenishPeriod;
            this.burstCapacity = burstCapacity;
            this.clock = clock;
        }

        @Override
        public Mono<Response> isAllowed(String routeId, String key) {
            long now = clock.millis();
            evictExpiredWindowsIfDue(now);
            String compositeKey = routeId + ":" + key;
            Window window = windows.compute(compositeKey, (ignored, existing) -> refreshWindow(existing, now));
            int remaining = Math.max(burstCapacity - window.requestCount(), 0);
            return Mono.just(new Response(window.requestCount() <= burstCapacity, Map.of(
                    LIMIT_HEADER, Integer.toString(burstCapacity),
                    REMAINING_HEADER, Integer.toString(remaining)
            )));
        }

        private void evictExpiredWindowsIfDue(long now) {
            long scheduledEviction = nextEvictionAt.get();
            if (now < scheduledEviction) {
                return;
            }

            if (!nextEvictionAt.compareAndSet(scheduledEviction, now + replenishPeriod.toMillis())) {
                return;
            }

            windows.entrySet().removeIf(entry -> now - entry.getValue().windowStartedAt() >= replenishPeriod.toMillis());
        }

        private Window refreshWindow(Window existing, long now) {
            if (existing == null || now - existing.windowStartedAt() >= replenishPeriod.toMillis()) {
                return new Window(now, 1);
            }

            return new Window(existing.windowStartedAt(), Math.min(existing.requestCount() + 1, burstCapacity + 1));
        }

        static final class Config {
        }

        private record Window(long windowStartedAt, int requestCount) {
        }
    }
}
