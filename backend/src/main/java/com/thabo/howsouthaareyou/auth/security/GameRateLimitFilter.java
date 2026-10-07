package com.thabo.howsouthaareyou.auth.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Component
@Order(-100)
public class GameRateLimitFilter extends OncePerRequestFilter {

    private final Cache<String, Bucket> startGameBuckets = Caffeine.newBuilder()
            .expireAfterAccess(15, TimeUnit.MINUTES)
            .maximumSize(100_000)
            .build();

    private final Cache<String, Bucket> roundScoreBuckets = Caffeine.newBuilder()
            .expireAfterAccess(15, TimeUnit.MINUTES)
            .maximumSize(100_000)
            .build();

    private final Cache<String, Bucket> completeGameBuckets = Caffeine.newBuilder()
            .expireAfterAccess(15, TimeUnit.MINUTES)
            .maximumSize(100_000)
            .build();

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();
        String method = request.getMethod();

        if (!path.startsWith("/api/v1/thirty-seconds/games") || !"POST".equals(method)) {
            filterChain.doFilter(request, response);
            return;
        }

        String userKey = resolveUserKey();

        if (userKey == null) {
            filterChain.doFilter(request, response);
            return;
        }

        if (path.endsWith("/start")) {
            if (!tryConsume(startGameBuckets, userKey, 5, Duration.ofMinutes(1))) {
                writeRateLimitResponse(response);
                return;
            }
        } else if (path.contains("/rounds/score")) {
            if (!tryConsume(roundScoreBuckets, userKey, 30, Duration.ofMinutes(1))) {
                writeRateLimitResponse(response);
                return;
            }
        } else if (path.endsWith("/complete")) {
            if (!tryConsume(completeGameBuckets, userKey, 10, Duration.ofMinutes(1))) {
                writeRateLimitResponse(response);
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean tryConsume(Cache<String, Bucket> cache, String key, int capacity, Duration period) {
        Bucket bucket = cache.get(key, k -> buildBucket(capacity, period));
        return bucket.tryConsume(1);
    }

    private Bucket buildBucket(int capacity, Duration refillPeriod) {
        return Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(capacity)
                        .refillGreedy(capacity, refillPeriod)
                        .build())
                .build();
    }

    private String resolveUserKey() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            return null;
        }
        return authentication.getName();
    }

    private void writeRateLimitResponse(HttpServletResponse response) throws IOException {
        response.setStatus(429);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(
                "{\"success\":false,\"message\":\"Too many requests. Please slow down.\"}");
    }
}