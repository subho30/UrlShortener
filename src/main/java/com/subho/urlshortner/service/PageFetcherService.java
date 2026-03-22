package com.subho.urlshortner.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.stream.Collectors;

@Service
@Slf4j
public class PageFetcherService {

    private static final int TIMEOUT_MS = 5000;
    private static final int MAX_CONTENT_LENGTH = 2000;

    public String fetchPageContent(String urlString) {
        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(TIMEOUT_MS);
            connection.setReadTimeout(TIMEOUT_MS);
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");

            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                log.warn("Failed to fetch page: {} — HTTP {}", urlString, responseCode);
                return null;
            }

            String content = new BufferedReader(new InputStreamReader(connection.getInputStream()))
                    .lines()
                    .collect(Collectors.joining("\n"));

            // Strip HTML tags — get plain text only
            content = content.replaceAll("<[^>]*>", " ")
                    .replaceAll("\\s+", " ")
                    .trim();

            // Truncate to avoid overwhelming the AI
            if (content.length() > MAX_CONTENT_LENGTH) {
                content = content.substring(0, MAX_CONTENT_LENGTH);
            }

            log.info("Fetched page content for: {} ({} chars)", urlString, content.length());
            return content;

        } catch (Exception e) {
            log.warn("Could not fetch page content for {}: {}", urlString, e.getMessage());
            return null;
        }
    }
}