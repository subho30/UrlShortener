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
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Controller
@Slf4j
@RequiredArgsConstructor
@Tag(name = "URL Shortener", description = "Endpoints for shortening URLs, redirecting, and analytics")
public class UrlController {

    private final UrlShortenerService urlShortenerService;
    private final RateLimiterService rateLimiterService;

    // ── Shorten a URL ──
    @Operation(summary = "Shorten a URL", description = "Generates a unique short code for a given URL with AI enrichment. Rate limited to 10 requests/minute per IP.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "URL shortened successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid URL format or unsafe URL detected"),
            @ApiResponse(responseCode = "429", description = "Rate limit exceeded")
    })
    @ResponseBody
    @PostMapping("/api/shorten")
    public ResponseEntity<ShortenResponse> shortenUrl(
            @Valid @RequestBody ShortenRequest request,
            HttpServletRequest httpRequest) {
        String clientIp = getClientIp(httpRequest);
        rateLimiterService.validateRateLimit(clientIp);
        ShortenResponse response = urlShortenerService.shortenUrl(request, clientIp);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ── Get analytics for a short code ──
    @Operation(summary = "Get link analytics", description = "Returns hit count, creation time, expiry, AI summary, category, tags, and safety score for a short code.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Analytics retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Short code not found")
    })
    @ResponseBody
    @GetMapping("/api/analytics/{shortCode}")
    public ResponseEntity<ShortenResponse> getAnalytics(@PathVariable String shortCode) {
        ShortenResponse response = urlShortenerService.getAnalytics(shortCode);
        return ResponseEntity.ok(response);
    }

    // ── Redirect to preview page ──
    @Operation(summary = "Redirect to preview page", description = "Validates the short code and redirects to the AI-powered link preview page before proceeding.")
    @ApiResponses({
            @ApiResponse(responseCode = "302", description = "Redirect to preview page"),
            @ApiResponse(responseCode = "404", description = "Short code not found or expired")
    })
    @ResponseBody
    @GetMapping("/{shortCode}")
    public ResponseEntity<Void> redirect(@PathVariable String shortCode) {
        urlShortenerService.getAnalytics(shortCode);
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.LOCATION, "/preview/" + shortCode);
        return ResponseEntity.status(HttpStatus.FOUND).headers(headers).build();
    }

    // ── Preview page ──
    @Operation(summary = "Link preview page", description = "Returns an HTML preview page with AI-generated title, summary, category, tags, and safety score before redirecting.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Preview page rendered successfully"),
            @ApiResponse(responseCode = "404", description = "Short code not found or expired")
    })
    @GetMapping("/preview/{shortCode}")
    public String preview(@PathVariable String shortCode, Model model) {
        ShortenResponse data = urlShortenerService.getAnalytics(shortCode);
        model.addAttribute("data", data);
        model.addAttribute("domain", data.getOriginalUrl()
                .replaceAll("https?://", "")
                .split("/")[0]);
        model.addAttribute("safetyColor", "SAFE".equals(data.getSafetyStatus()) ? "#22c55e" : "#ef4444");
        model.addAttribute("safetyIcon", "SAFE".equals(data.getSafetyStatus()) ? "✅" : "🚨");
        model.addAttribute("safetyText", "SAFE".equals(data.getSafetyStatus())
                ? "Safe to visit (" + data.getSafetyScore() + "/100)"
                : "Potentially unsafe (" + data.getSafetyScore() + "/100)");
        return "preview";
    }

    // ── Direct redirect — skips preview ──
    @Operation(summary = "Direct redirect", description = "Skips the preview page and redirects directly to the original URL. Use for programmatic access.")
    @ApiResponses({
            @ApiResponse(responseCode = "302", description = "Redirect to original URL"),
            @ApiResponse(responseCode = "404", description = "Short code not found or expired")
    })
    @ResponseBody
    @GetMapping("/go/{shortCode}")
    public ResponseEntity<Void> directRedirect(@PathVariable String shortCode) {
        String originalUrl = urlShortenerService.resolveShortCode(shortCode);
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.LOCATION, originalUrl);
        return ResponseEntity.status(HttpStatus.FOUND).headers(headers).build();
    }

    // ── Deactivate a short link (soft delete) ──
    @Operation(summary = "Deactivate a short link", description = "Soft deletes a short link — it stops working but analytics and history are preserved.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Link deactivated successfully"),
            @ApiResponse(responseCode = "404", description = "Short code not found")
    })
    @ResponseBody
    @DeleteMapping("/api/links/{shortCode}")
    public ResponseEntity<Map<String, Object>> deactivateUrl(@PathVariable String shortCode) {
        urlShortenerService.deactivateUrl(shortCode);
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Link '" + shortCode + "' has been deactivated successfully");
        response.put("shortCode", shortCode);
        response.put("timestamp", LocalDateTime.now());
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