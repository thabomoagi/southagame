package com.thabo.howsouthaareyou.auth.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Service
public class LoginAttemptService {

    private final Cache<String, Bucket> accountBuckets = Caffeine.newBuilder()
            .expireAfterAccess(15, TimeUnit.MINUTES)
            .maximumSize(100_000)
            .build();

    public boolean isBlocked(String identifier) {
        if (identifier == null || identifier.isBlank())
            return false;
        String key = identifier.toLowerCase();
        Bucket bucket = accountBuckets.get(key, k -> buildBucket(5, Duration.ofMinutes(10)));
        return bucket.getAvailableTokens() == 0;
    }

    public void loginFailed(String identifier) {
        if (identifier == null || identifier.isBlank())
            return;
        String key = identifier.toLowerCase();
        Bucket bucket = accountBuckets.get(key, k -> buildBucket(5, Duration.ofMinutes(10)));
        bucket.tryConsume(1);
    }

    public void loginSucceeded(String identifier) {
        if (identifier == null || identifier.isBlank())
            return;
        accountBuckets.invalidate(identifier.toLowerCase());
    }

    private Bucket buildBucket(int capacity, Duration refillPeriod) {
        return Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(capacity)
                        .refillGreedy(capacity, refillPeriod)
                        .build())
                .build();
    }
}