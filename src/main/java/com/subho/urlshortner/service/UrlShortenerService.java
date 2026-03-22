package com.subho.urlshortner.service;

import com.subho.urlshortner.exception.UrlUnsafeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.subho.urlshortner.dto.ShortenRequest;
import com.subho.urlshortner.dto.ShortenResponse;
import com.subho.urlshortner.exception.UrlNotFoundException;
import com.subho.urlshortner.model.UrlMapping;
import com.subho.urlshortner.repository.UrlMappingRepository;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class UrlShortenerService {

    private final UrlMappingRepository urlMappingRepository;
    private final AiEnrichmentService aiEnrichmentService;
    private final PageFetcherService pageFetcherService;

    @Value("${app.base-url}")
    private String baseUrl;

    @Value("${app.short-code-length}")
    private int shortCodeLength;

    @Value("${app.default-expiry-days}")
    private int defaultExpiryDays;

    private static final String BASE62_CHARS =
            "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    // ── Shorten a URL ──
    public ShortenResponse shortenUrl(ShortenRequest request, String clientIp) {
        String shortCode = generateUniqueShortCode();

        // Fetch page content for AI enrichment
        String pageContent = pageFetcherService.fetchPageContent(request.getOriginalUrl());

        // Run AI enrichment
        Map<String, Object> aiData = aiEnrichmentService.enrichUrl(request.getOriginalUrl(), pageContent);

        // Check if URL is unsafe — reject immediately
        String safetyStatus = (String) aiData.getOrDefault("safetyStatus", "SAFE");
        if ("UNSAFE".equals(safetyStatus)) {
            throw new UrlUnsafeException("URL flagged as unsafe: " +
                    aiData.getOrDefault("safetyReason", "Malicious or spammy content detected"));
        }

        // Use AI suggested expiry if user didn't provide one
        int expiryDays;
        if (request.getExpiryDays() != null && request.getExpiryDays() > 0) {
            expiryDays = request.getExpiryDays();
        } else {
            expiryDays = (Integer) aiData.getOrDefault("suggestedExpiryDays", defaultExpiryDays);
        }

        UrlMapping urlMapping = UrlMapping.builder()
                .shortCode(shortCode)
                .originalUrl(request.getOriginalUrl())
                .expiresAt(LocalDateTime.now().plusDays(expiryDays))
                .createdByIp(clientIp)
                .summary((String) aiData.getOrDefault("summary", null))
                .title((String) aiData.getOrDefault("title", null))
                .category((String) aiData.getOrDefault("category", null))
                .tags((String) aiData.getOrDefault("tags", null))
                .safetyStatus(safetyStatus)
                .safetyScore((Integer) aiData.getOrDefault("safetyScore", null))
                .suggestedExpiryDays((Integer) aiData.getOrDefault("suggestedExpiryDays", null))
                .expiryReason((String) aiData.getOrDefault("expiryReason", null))
                .build();

        urlMappingRepository.save(urlMapping);
        log.info("Shortened URL: {} -> {}", request.getOriginalUrl(), shortCode);

        return buildResponse(urlMapping);
    }

    // ── Resolve a short code to original URL ──
    @Transactional
    public String resolveShortCode(String shortCode) {
        UrlMapping urlMapping = urlMappingRepository
                .findActiveByShortCode(shortCode, LocalDateTime.now())
                .orElseThrow(() -> new UrlNotFoundException(
                        "Short URL not found or expired: " + shortCode));

        urlMappingRepository.incrementHitCount(shortCode, LocalDateTime.now());
        log.info("Resolved short code: {} -> {}", shortCode, urlMapping.getOriginalUrl());

        return urlMapping.getOriginalUrl();
    }

    // ── Get analytics for a short code ──
    public ShortenResponse getAnalytics(String shortCode) {
        UrlMapping urlMapping = urlMappingRepository
                .findByShortCode(shortCode)
                .orElseThrow(() -> new UrlNotFoundException(
                        "Short URL not found: " + shortCode));

        return buildResponse(urlMapping);
    }

    // ── Generate a unique Base62 short code ──
    private String generateUniqueShortCode() {
        String shortCode;
        int attempts = 0;
        do {
            shortCode = generateShortCode();
            attempts++;
            if (attempts > 10) {
                throw new RuntimeException("Failed to generate unique short code");
            }
        } while (urlMappingRepository.existsByShortCode(shortCode));
        return shortCode;
    }

    private String generateShortCode() {
        Random random = new Random();
        StringBuilder sb = new StringBuilder(shortCodeLength);
        for (int i = 0; i < shortCodeLength; i++) {
            sb.append(BASE62_CHARS.charAt(random.nextInt(BASE62_CHARS.length())));
        }
        return sb.toString();
    }

    // ── Build response from entity ──
    private ShortenResponse buildResponse(UrlMapping urlMapping) {
        return ShortenResponse.builder()
                .shortCode(urlMapping.getShortCode())
                .shortUrl(baseUrl + "/" + urlMapping.getShortCode())
                .originalUrl(urlMapping.getOriginalUrl())
                .createdAt(urlMapping.getCreatedAt())
                .expiresAt(urlMapping.getExpiresAt())
                .hitCount(urlMapping.getHitCount())
                .summary(urlMapping.getSummary())
                .title(urlMapping.getTitle())
                .category(urlMapping.getCategory())
                .tags(urlMapping.getTags())
                .safetyStatus(urlMapping.getSafetyStatus())
                .safetyScore(urlMapping.getSafetyScore())
                .suggestedExpiryDays(urlMapping.getSuggestedExpiryDays())
                .expiryReason(urlMapping.getExpiryReason())
                .build();
    }
}