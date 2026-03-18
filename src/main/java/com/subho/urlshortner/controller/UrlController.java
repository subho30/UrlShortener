package com.subho.urlshortner.controller;

import com.subho.urlshortner.dto.ShortenRequest;
import com.subho.urlshortner.dto.ShortenResponse;
import com.subho.urlshortner.service.RateLimiterService;
import com.subho.urlshortner.service.UrlShortenerService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@Slf4j
@RequiredArgsConstructor
@Tag(name = "URL Shortener", description = "Endpoints for shortening URLs, redirecting, and analytics")
public class UrlController {

    private final UrlShortenerService urlShortenerService;
    private final RateLimiterService rateLimiterService;

    // ── Shorten a URL ──
    @Operation(summary = "Shorten a URL", description = "Generates a unique short code for a given URL. Rate limited to 10 requests/minute per IP.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "URL shortened successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid URL format"),
        @ApiResponse(responseCode = "429", description = "Rate limit exceeded")
    })
    @PostMapping("/api/shorten")
    public ResponseEntity<ShortenResponse> shortenUrl(
            @Valid @RequestBody ShortenRequest request,
            HttpServletRequest httpRequest) {

        String clientIp = getClientIp(httpRequest);
        rateLimiterService.validateRateLimit(clientIp);

        ShortenResponse response = urlShortenerService.shortenUrl(request, clientIp);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ── Redirect to original URL ──
    @Operation(summary = "Redirect to original URL", description = "Resolves a short code and redirects to the original URL.")
    @ApiResponses({
        @ApiResponse(responseCode = "302", description = "Redirect to original URL"),
        @ApiResponse(responseCode = "404", description = "Short code not found or expired")
    })
    @GetMapping("/{shortCode}")
    public ResponseEntity<Void> redirect(@PathVariable String shortCode) {
        String originalUrl = urlShortenerService.resolveShortCode(shortCode);

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.LOCATION, originalUrl);
        return ResponseEntity.status(HttpStatus.FOUND).headers(headers).build();
    }

    // ── Get analytics for a short code ──
    @Operation(summary = "Get link analytics", description = "Returns hit count, creation time, expiry, and last accessed time for a short code.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Analytics retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Short code not found")
    })
    @GetMapping("/api/analytics/{shortCode}")
    public ResponseEntity<ShortenResponse> getAnalytics(
            @PathVariable String shortCode) {
        ShortenResponse response = urlShortenerService.getAnalytics(shortCode);
        return ResponseEntity.ok(response);
    }

    // ── Extract real client IP ──
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}