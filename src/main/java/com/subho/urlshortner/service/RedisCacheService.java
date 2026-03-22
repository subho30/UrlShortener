package com.subho.urlshortner.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.subho.urlshortner.dto.ShortenResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisCacheService {

    private final RedisTemplate<String, String> redisTemplate;
    private static final String URL_PREFIX = "url:";
    private static final Duration TTL = Duration.ofHours(1);

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    // ── Save to cache ──
    public void cacheUrl(String shortCode, ShortenResponse response) {
        try {
            String json = objectMapper.writeValueAsString(response);
            redisTemplate.opsForValue().set(URL_PREFIX + shortCode, json, TTL);
            log.info("Cached short code: {}", shortCode);
        } catch (Exception e) {
            log.warn("Failed to cache short code {}: {}", shortCode, e.getMessage());
        }
    }

    // ── Get from cache ──
    public ShortenResponse getCachedUrl(String shortCode) {
        try {
            String json = redisTemplate.opsForValue().get(URL_PREFIX + shortCode);
            if (json != null) {
                log.info("Cache hit for short code: {}", shortCode);
                return objectMapper.readValue(json, ShortenResponse.class);
            }
        } catch (Exception e) {
            log.warn("Failed to get cached short code {}: {}", shortCode, e.getMessage());
        }
        return null;
    }

    // ── Evict from cache ──
    public void evictCache(String shortCode) {
        try {
            redisTemplate.delete(URL_PREFIX + shortCode);
            log.info("Evicted cache for short code: {}", shortCode);
        } catch (Exception e) {
            log.warn("Failed to evict cache for {}: {}", shortCode, e.getMessage());
        }
    }
}