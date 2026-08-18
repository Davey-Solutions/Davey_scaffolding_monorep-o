package com.daveysolutions.gateway;

/**
 * Captures the start time and request count for a single fixed rate-limit window.
 *
 * @param windowStartedAt the epoch-millisecond timestamp when the window started
 * @param requestCount the number of requests seen in the window
 */
record RateLimitWindow(long windowStartedAt, int requestCount) {
}
