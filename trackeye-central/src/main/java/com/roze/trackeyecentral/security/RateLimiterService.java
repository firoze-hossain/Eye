// src/main/java/com/trackeye/security/RateLimiterService.java
package com.roze.trackeyecentral.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
public class RateLimiterService {

    @Value("${trackeye.rate-limiting.enabled:true}")
    private boolean rateLimitingEnabled;

    @Value("${trackeye.rate-limiting.requests-per-minute:60}")
    private int requestsPerMinute;

    @Value("${trackeye.rate-limiting.burst-capacity:100}")
    private int burstCapacity;

    private final Cache<String, AtomicInteger> requestCounts = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(1))
            .maximumSize(10000)
            .build();

    public boolean isRateLimited(String clientId) {
        if (!rateLimitingEnabled) {
            return false;
        }

        AtomicInteger count = requestCounts.get(clientId, k -> new AtomicInteger(0));
        int currentCount = count.incrementAndGet();

        if (currentCount > burstCapacity) {
            log.warn("Rate limit exceeded for client: {}", clientId);
            return true;
        }

        return currentCount > requestsPerMinute;
    }
}