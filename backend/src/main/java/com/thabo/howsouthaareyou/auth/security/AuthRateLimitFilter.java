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
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Component
@Order(-200)
public class AuthRateLimitFilter extends OncePerRequestFilter {

    private final Cache<String, Bucket> ipLoginBuckets = Caffeine.newBuilder()
            .expireAfterAccess(15, TimeUnit.MINUTES)
            .maximumSize(100_000)
            .build();

    private final Cache<String, Bucket> registerBuckets = Caffeine.newBuilder()
            .expireAfterAccess(15, TimeUnit.MINUTES)
            .maximumSize(100_000)
            .build();

    private final Cache<String, Bucket> forgotPasswordBuckets = Caffeine.newBuilder()
            .expireAfterAccess(15, TimeUnit.MINUTES)
            .maximumSize(100_000)
            .build();

    private final Cache<String, Bucket> refreshBuckets = Caffeine.newBuilder()
            .expireAfterAccess(15, TimeUnit.MINUTES)
            .maximumSize(100_000)
            .build();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();

        if (!path.startsWith("/api/v1/auth/")) {
            filterChain.doFilter(request, response);
            return;
        }

        String ip = resolveClientIp(request);

        if (path.endsWith("/auth/login")) {
            if (!tryConsume(ipLoginBuckets, ip, 10, Duration.ofMinutes(1))) {
                writeRateLimitResponse(response);
                return;
            }
        } else if (path.endsWith("/auth/register")) {
            if (!tryConsume(registerBuckets, ip, 3, Duration.ofMinutes(10))) {
                writeRateLimitResponse(response);
                return;
            }
        } else if (path.endsWith("/auth/forgot-password")) {
            if (!tryConsume(forgotPasswordBuckets, ip, 3, Duration.ofMinutes(10))) {
                writeRateLimitResponse(response);
                return;
            }
        } else if (path.endsWith("/auth/refresh")) {
            if (!tryConsume(refreshBuckets, ip, 30, Duration.ofMinutes(1))) {
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

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private void writeRateLimitResponse(HttpServletResponse response) throws IOException {
        response.setStatus(429);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"success\":false,\"message\":\"Too many attempts. Please try again later.\"}");
    }
}