package org.donorly.backend.security;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * In-memory sliding-window rate limiter for auth endpoints.
 * Per-instance only (acceptable for a small Cloud Run fleet); swap for a
 * shared store (Redis) if instance count grows.
 */
@Component
public class LoginRateLimiter {

    private static final int MAX_FAILURES = 5;
    private static final Duration WINDOW = Duration.ofMinutes(15);

    private record Bucket(Instant windowStart, AtomicInteger count) {}

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    /** True if the key is currently locked out. */
    public boolean isBlocked(String key) {
        Bucket bucket = buckets.get(normalize(key));
        if (bucket == null) return false;
        if (bucket.windowStart().plus(WINDOW).isBefore(Instant.now())) {
            buckets.remove(normalize(key));
            return false;
        }
        return bucket.count().get() >= MAX_FAILURES;
    }

    /** Minutes until the lockout for this key expires (0 when not blocked). */
    public long minutesUntilUnblocked(String key) {
        Bucket bucket = buckets.get(normalize(key));
        if (bucket == null) return 0;
        long seconds = Duration.between(Instant.now(), bucket.windowStart().plus(WINDOW)).getSeconds();
        return Math.max(0, (seconds + 59) / 60);
    }

    public void recordFailure(String key) {
        cleanup();
        buckets.compute(normalize(key), (k, existing) -> {
            if (existing == null || existing.windowStart().plus(WINDOW).isBefore(Instant.now())) {
                return new Bucket(Instant.now(), new AtomicInteger(1));
            }
            existing.count().incrementAndGet();
            return existing;
        });
    }

    public void reset(String key) {
        buckets.remove(normalize(key));
    }

    private String normalize(String key) {
        return key == null ? "" : key.trim().toLowerCase();
    }

    private void cleanup() {
        if (buckets.size() < 10_000) return;
        Instant cutoff = Instant.now().minus(WINDOW);
        Iterator<Map.Entry<String, Bucket>> it = buckets.entrySet().iterator();
        while (it.hasNext()) {
            if (it.next().getValue().windowStart().isBefore(cutoff)) {
                it.remove();
            }
        }
    }
}
