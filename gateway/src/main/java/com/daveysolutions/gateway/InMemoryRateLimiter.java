package com.daveysolutions.gateway;

import org.springframework.cloud.gateway.filter.ratelimit.RateLimiter;
import org.springframework.cloud.gateway.support.AbstractStatefulConfigurable;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Fixed-window in-memory {@link RateLimiter} implementation for basic gateway throttling.
 */
class InMemoryRateLimiter extends AbstractStatefulConfigurable<InMemoryRateLimiterConfig>
        implements RateLimiter<InMemoryRateLimiterConfig> {

    private static final String REMAINING_HEADER = "X-RateLimit-Remaining";
    private static final String LIMIT_HEADER = "X-RateLimit-Limit";

    private final Duration replenishPeriod;
    private final int burstCapacity;
    private final Clock clock;
    private final ConcurrentMap<String, RateLimitWindow> windows = new ConcurrentHashMap<>();
    private final AtomicLong nextEvictionAt = new AtomicLong();

    /**
     * Creates a rate limiter with the supplied fixed window settings.
     *
     * @param replenishPeriod the duration of each rate-limit window
     * @param burstCapacity the maximum number of requests allowed in a window
     * @param clock the clock used to evaluate window expiry
     */
    InMemoryRateLimiter(Duration replenishPeriod, int burstCapacity, Clock clock) {
        super(InMemoryRateLimiterConfig.class);
        this.replenishPeriod = replenishPeriod;
        this.burstCapacity = burstCapacity;
        this.clock = clock;
    }

    /**
     * Determines whether the current request should be allowed for the supplied route/key pair.
     *
     * @param routeId the gateway route identifier
     * @param key the resolved client key
     * @return the rate-limit decision and response headers for the request
     */
    @Override
    public Mono<Response> isAllowed(String routeId, String key) {
        long now = clock.millis();
        evictExpiredWindowsIfDue(now);
        String compositeKey = routeId + ":" + key;
        RateLimitWindow window = windows.compute(compositeKey, (ignored, existing) -> refreshWindow(existing, now));
        int remaining = Math.max(burstCapacity - window.requestCount(), 0);
        return Mono.just(new Response(window.requestCount() <= burstCapacity, Map.of(
                LIMIT_HEADER, Integer.toString(burstCapacity),
                REMAINING_HEADER, Integer.toString(remaining)
        )));
    }

    private void evictExpiredWindowsIfDue(long now) {
        long scheduledEviction = nextEvictionAt.get();
        if (shouldSkipEviction(now, scheduledEviction)) {
            return;
        }

        windows.entrySet().removeIf(entry -> hasWindowExpired(entry.getValue(), now));
    }

    private boolean shouldSkipEviction(long now, long scheduledEviction) {
        return now < scheduledEviction
                || !nextEvictionAt.compareAndSet(scheduledEviction, now + replenishPeriod.toMillis());
    }

    private RateLimitWindow refreshWindow(RateLimitWindow existing, long now) {
        if (existing == null || hasWindowExpired(existing, now)) {
            return new RateLimitWindow(now, 1);
        }

        return new RateLimitWindow(existing.windowStartedAt(), Math.min(existing.requestCount() + 1, burstCapacity + 1));
    }

    private boolean hasWindowExpired(RateLimitWindow window, long now) {
        return now - window.windowStartedAt() >= replenishPeriod.toMillis();
    }
}
