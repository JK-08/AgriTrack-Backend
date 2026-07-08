package AgriTrackBackend.SECURITY;

import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Simple in-memory sliding-window rate limiter, keyed by an arbitrary
 * string (typically "ip:category"). Good enough for a single backend
 * instance; if this app is ever horizontally scaled, swap this for a
 * Redis-backed limiter (e.g. Bucket4j + Redis) — callers only interact
 * with {@link #tryConsume}, so that swap wouldn't touch RateLimitFilter.
 */
public class RateLimiter {

    private final ConcurrentHashMap<String, Deque<Long>> hits = new ConcurrentHashMap<>();

    /** @return true if this call is allowed under the limit, false if it should be rejected (429). */
    public boolean tryConsume(String key, int maxRequests, long windowMillis) {
        long now = System.currentTimeMillis();
        Deque<Long> timestamps = hits.computeIfAbsent(key, k -> new ConcurrentLinkedDeque<>());

        synchronized (timestamps) {
            while (!timestamps.isEmpty() && now - timestamps.peekFirst() > windowMillis) {
                timestamps.pollFirst();
            }
            if (timestamps.size() >= maxRequests) {
                return false;
            }
            timestamps.addLast(now);
            return true;
        }
    }
}
