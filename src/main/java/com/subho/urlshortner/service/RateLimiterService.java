package com.subho.urlshortner.service;

import com.subho.urlshortner.exception.RateLimitException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class RateLimiterService {

    // Max requests per window per IP
    private static final int MAX_REQUESTS = 10;

    // Window size in minutes
    private static final int WINDOW_MINUTES = 1;

    // IP -> request tracking
    private final Map<String, RequestWindow> requestWindowMap = new ConcurrentHashMap<>();

    public void validateRateLimit(String clientIp) {
        requestWindowMap.merge(clientIp, new RequestWindow(), (existing, newWindow) -> {
            // If window has expired, reset it
            if (existing.isExpired()) {
                return new RequestWindow();
            }
            return existing;
        });

        RequestWindow window = requestWindowMap.get(clientIp);
        window.increment();

        if (window.getCount() > MAX_REQUESTS) {
            log.warn("Rate limit exceeded for IP: {}", clientIp);
            throw new RateLimitException(
                "Rate limit exceeded. Max " + MAX_REQUESTS +
                " requests per " + WINDOW_MINUTES + " minute(s). Please try again later."
            );
        }
    }

    // Inner class to track requests per window
    private static class RequestWindow {
        private int count = 0;
        private final LocalDateTime windowStart = LocalDateTime.now();

        public void increment() {
            this.count++;
        }

        public int getCount() {
            return count;
        }

        public boolean isExpired() {
            return LocalDateTime.now().isAfter(
                windowStart.plusMinutes(WINDOW_MINUTES)
            );
        }
    }
}