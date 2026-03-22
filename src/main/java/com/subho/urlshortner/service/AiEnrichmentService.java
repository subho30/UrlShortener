package com.subho.urlshortner.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiEnrichmentService {

    private final ChatClient.Builder chatClientBuilder;



    // ── Master enrichment method ──
    public Map<String, Object> enrichUrl(String url, String pageContent) {
        Map<String, Object> result = new HashMap<>();

        // Step 1 — Safety Check
        log.info("Running safety check for URL: {}", url);
        Map<String, Object> safetyResult = checkSafety(url);
        result.putAll(safetyResult);

        // If unsafe — return immediately, no further processing
        String safetyStatus = (String) safetyResult.get("safetyStatus");
        if ("UNSAFE".equals(safetyStatus)) {
            log.warn("URL flagged as unsafe: {}", url);
            return result;
        }

        // Step 2 — Enrich with title, summary, category, tags, expiry
        log.info("Running content enrichment for URL: {}", url);
        Map<String, Object> contentResult = enrichContent(url, pageContent);
        result.putAll(contentResult);

        return result;
    }

    // ── Safety Check ──
    private Map<String, Object> checkSafety(String url) {
        Map<String, Object> result = new HashMap<>();
        try {
            ChatClient chatClient = chatClientBuilder.build();
            String prompt = """
                    Analyze this URL for safety: %s
                    
                    Respond ONLY with this exact JSON format, no other text:
                    {"safetyStatus":"SAFE","safetyScore":95,"safetyReason":"Legitimate website"}
                    
                    safetyStatus must be either SAFE or UNSAFE.
                    safetyScore must be 0-100 (100 = completely safe).
                    safetyReason must be a brief explanation.
                    """.formatted(url);

            String response = chatClient.prompt(prompt).call().content();
            response = extractJson(response);

            result.put("safetyStatus", extractField(response, "safetyStatus", "SAFE"));
            result.put("safetyScore", Integer.parseInt(extractField(response, "safetyScore", "50")));
            result.put("safetyReason", extractField(response, "safetyReason", "Unable to determine"));
        } catch (Exception e) {
            log.error("Safety check failed: {}", e.getMessage());
            result.put("safetyStatus", "SAFE");
            result.put("safetyScore", 50);
            result.put("safetyReason", "Safety check unavailable");
        }
        return result;
    }

    // ── Content Enrichment ──
    private Map<String, Object> enrichContent(String url, String pageContent) {
        Map<String, Object> result = new HashMap<>();
        try {
            ChatClient chatClient = chatClientBuilder.build();
            String contentSnippet = pageContent != null && pageContent.length() > 500
                    ? pageContent.substring(0, 500)
                    : (pageContent != null ? pageContent : "No content available");

            String prompt = """
                    Analyze this URL and content:
                    URL: %s
                    Content snippet: %s
                    
                    Respond ONLY with this exact JSON format, no other text:
                    {"title":"Page Title","summary":"One sentence summary","category":"Technology","tags":"Java,Spring Boot,AI","suggestedExpiryDays":30,"expiryReason":"Technical documentation remains relevant long-term"}
                    
                    Rules:
                    - title: human readable page title (max 100 chars)
                    - summary: one sentence description (max 200 chars)
                    - category: single category from [Technology, News, Finance, Health, Education, Entertainment, Shopping, Social, Sports, Other]
                    - tags: comma-separated, max 3 tags
                    - suggestedExpiryDays: number between 1-365 based on content type
                    - expiryReason: brief reason for suggested expiry
                    """.formatted(url, contentSnippet);

            String response = chatClient.prompt(prompt).call().content();
            response = extractJson(response);

            result.put("title", extractField(response, "title", "Unknown Title"));
            result.put("summary", extractField(response, "summary", "No summary available"));
            result.put("category", extractField(response, "category", "Other"));
            result.put("tags", extractField(response, "tags", ""));
            result.put("suggestedExpiryDays", Integer.parseInt(extractField(response, "suggestedExpiryDays", "30")));
            result.put("expiryReason", extractField(response, "expiryReason", "Default expiry"));
        } catch (Exception e) {
            log.error("Content enrichment failed: {}", e.getMessage());
            result.put("title", "Unknown Title");
            result.put("summary", "Content analysis unavailable");
            result.put("category", "Other");
            result.put("tags", "");
            result.put("suggestedExpiryDays", 30);
            result.put("expiryReason", "Default expiry applied");
        }
        return result;
    }

    // ── Helper: Extract JSON from response ──
    private String extractJson(String response) {
        int start = response.indexOf("{");
        int end = response.lastIndexOf("}");
        if (start != -1 && end != -1) {
            return response.substring(start, end + 1);
        }
        return response;
    }

    // ── Helper: Extract field from JSON string ──
    private String extractField(String json, String field, String defaultValue) {
        try {
            String search = "\"" + field + "\":";
            int start = json.indexOf(search);
            if (start == -1) return defaultValue;
            start += search.length();

            // Skip whitespace
            while (start < json.length() && json.charAt(start) == ' ') start++;

            // Check if value is a string or number
            if (json.charAt(start) == '"') {
                start++; // skip opening quote
                int end = json.indexOf("\"", start);
                return end != -1 ? json.substring(start, end) : defaultValue;
            } else {
                // Number or boolean
                int end = start;
                while (end < json.length() && json.charAt(end) != ',' && json.charAt(end) != '}') end++;
                return json.substring(start, end).trim();
            }
        } catch (Exception e) {
            return defaultValue;
        }
    }
}