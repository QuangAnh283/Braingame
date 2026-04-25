package org.example.quizizz.common.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class RateLimitService {

    private static final String KEY_PREFIX = "rate-limit:";
    private static final String OP_FALLBACK_PREFIX = "local:";
    private final Map<String, WindowCounter> counters = new ConcurrentHashMap<>();
    private static final int CLEANUP_THRESHOLD = 10_000;
    private final RedisTemplate<String, Object> redisTemplate;

    public boolean allow(String key, int maxRequests, Duration window) {
        try {
            return allowRedis(key, maxRequests, window);
        } catch (Exception e) {
            log.warn("Redis rate limit unavailable, using local fallback for key={}", key);
            return allowLocal(OP_FALLBACK_PREFIX + key, maxRequests, window);
        }
    }

    private boolean allowRedis(String key, int maxRequests, Duration window) {
        String redisKey = KEY_PREFIX + key;
        Long count = redisTemplate.opsForValue().increment(redisKey);
        if (count == null) {
            return false;
        }
        if (count == 1) {
            redisTemplate.expire(redisKey, window.toMillis(), TimeUnit.MILLISECONDS);
        }
        return count <= maxRequests;
    }

    private boolean allowLocal(String key, int maxRequests, Duration window) {
        long now = System.currentTimeMillis();
        long windowMs = window.toMillis();

        if (counters.size() > CLEANUP_THRESHOLD) {
            cleanupExpired(now);
        }

        WindowCounter counter = counters.compute(key, (ignored, current) -> {
            if (current == null || now >= current.windowExpiresAt) {
                return new WindowCounter(now + windowMs);
            }
            return current;
        });

        return counter.count.incrementAndGet() <= maxRequests;
    }

    private void cleanupExpired(long now) {
        counters.entrySet().removeIf(entry -> now >= entry.getValue().windowExpiresAt);
    }

    private static class WindowCounter {
        private final AtomicInteger count = new AtomicInteger(0);
        private final long windowExpiresAt;

        private WindowCounter(long windowExpiresAt) {
            this.windowExpiresAt = windowExpiresAt;
        }
    }
}
