package com.arvyax.journal.config;

import org.springframework.stereotype.Component;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple sliding-window rate limiter.
 * Limits each userId to maxRequests per windowSeconds on the analyze endpoint.
 */
@Component
public class RateLimiter {

    private static final int MAX_REQUESTS = 10;
    private static final long WINDOW_SECONDS = 60;

    // userId -> list of request timestamps (epoch seconds)
    private final ConcurrentHashMap<String, java.util.Deque<Long>> requestLog = new ConcurrentHashMap<>();

    public boolean isAllowed(String key) {
        long now = Instant.now().getEpochSecond();
        requestLog.putIfAbsent(key, new java.util.ArrayDeque<>());
        java.util.Deque<Long> timestamps = requestLog.get(key);

        synchronized (timestamps) {
            // Remove timestamps outside the window
            while (!timestamps.isEmpty() && now - timestamps.peekFirst() >= WINDOW_SECONDS) {
                timestamps.pollFirst();
            }
            if (timestamps.size() < MAX_REQUESTS) {
                timestamps.addLast(now);
                return true;
            }
            return false;
        }
    }
}
