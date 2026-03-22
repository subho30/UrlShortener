package com.subho.urlshortner.service;

import com.subho.urlshortner.dto.ShortenRequest;
import com.subho.urlshortner.dto.ShortenResponse;
import com.subho.urlshortner.exception.CustomAliasAlreadyTakenException;
import com.subho.urlshortner.exception.UrlNotFoundException;
import com.subho.urlshortner.exception.UrlUnsafeException;
import com.subho.urlshortner.model.UrlMapping;
import com.subho.urlshortner.repository.UrlMappingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UrlShortenerService Unit Tests")
class UrlShortenerServiceTest {

    @Mock
    private UrlMappingRepository urlMappingRepository;

    @Mock
    private AiEnrichmentService aiEnrichmentService;

    @Mock
    private PageFetcherService pageFetcherService;

    @Mock
    private RedisCacheService redisCacheService;

    @InjectMocks
    private UrlShortenerService urlShortenerService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(urlShortenerService, "baseUrl", "http://localhost:8080");
        ReflectionTestUtils.setField(urlShortenerService, "shortCodeLength", 6);
        ReflectionTestUtils.setField(urlShortenerService, "defaultExpiryDays", 30);
    }

    // ── Helper ──
    private Map<String, Object> mockSafeAiData() {
        return Map.of(
                "safetyStatus", "SAFE",
                "safetyScore", 95,
                "safetyReason", "Legitimate website",
                "title", "Test Title",
                "summary", "Test summary",
                "category", "Technology",
                "tags", "Java,Spring",
                "suggestedExpiryDays", 30,
                "expiryReason", "Standard expiry"
        );
    }

    private UrlMapping mockUrlMapping(String shortCode) {
        return UrlMapping.builder()
                .id(1L)
                .shortCode(shortCode)
                .originalUrl("https://github.com/subho30")
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusDays(30))
                .hitCount(0L)
                .isActive(true)
                .safetyStatus("SAFE")
                .safetyScore(95)
                .title("Test Title")
                .summary("Test summary")
                .category("Technology")
                .tags("Java,Spring")
                .suggestedExpiryDays(30)
                .expiryReason("Standard expiry")
                .build();
    }

    // ── Shorten URL Tests ──

    @Test
    @DisplayName("Should shorten URL and return response with short code")
    void shouldShortenUrl() {
        ShortenRequest request = new ShortenRequest();
        request.setOriginalUrl("https://github.com/subho30");

        when(pageFetcherService.fetchPageContent(any())).thenReturn("page content");
        when(aiEnrichmentService.enrichUrl(any(), any())).thenReturn(mockSafeAiData());
        when(urlMappingRepository.existsByShortCode(any())).thenReturn(false);
        when(urlMappingRepository.save(any())).thenAnswer(i -> {
            UrlMapping mapping = i.getArgument(0);
            if (mapping.getHitCount() == null) mapping.setHitCount(0L);
            return mapping;
        });

        ShortenResponse response = urlShortenerService.shortenUrl(request, "127.0.0.1");

        assertThat(response).isNotNull();
        assertThat(response.getShortCode()).isNotBlank();
        assertThat(response.getShortCode()).hasSize(6);
        assertThat(response.getOriginalUrl()).isEqualTo("https://github.com/subho30");
        assertThat(response.getShortUrl()).startsWith("http://localhost:8080/");
        assertThat(response.getSafetyStatus()).isEqualTo("SAFE");
        assertThat(response.getHitCount()).isEqualTo(0L);
        verify(urlMappingRepository, times(1)).save(any());
        verify(redisCacheService, times(1)).cacheUrl(any(), any());
    }

    @Test
    @DisplayName("Should use custom alias when provided")
    void shouldUseCustomAlias() {
        ShortenRequest request = new ShortenRequest();
        request.setOriginalUrl("https://github.com/subho30");
        request.setCustomAlias("my-github");

        when(urlMappingRepository.existsByShortCode("my-github")).thenReturn(false);
        when(pageFetcherService.fetchPageContent(any())).thenReturn("page content");
        when(aiEnrichmentService.enrichUrl(any(), any())).thenReturn(mockSafeAiData());
        when(urlMappingRepository.save(any())).thenAnswer(i -> {
            UrlMapping mapping = i.getArgument(0);
            if (mapping.getHitCount() == null) mapping.setHitCount(0L);
            return mapping;
        });

        ShortenResponse response = urlShortenerService.shortenUrl(request, "127.0.0.1");

        assertThat(response.getShortCode()).isEqualTo("my-github");
    }

    @Test
    @DisplayName("Should throw CustomAliasAlreadyTakenException when alias is taken")
    void shouldThrowWhenCustomAliasAlreadyTaken() {
        ShortenRequest request = new ShortenRequest();
        request.setOriginalUrl("https://github.com/subho30");
        request.setCustomAlias("my-github");

        when(urlMappingRepository.existsByShortCode("my-github")).thenReturn(true);

        assertThatThrownBy(() -> urlShortenerService.shortenUrl(request, "127.0.0.1"))
                .isInstanceOf(CustomAliasAlreadyTakenException.class)
                .hasMessageContaining("my-github");
    }

    @Test
    @DisplayName("Should throw UrlUnsafeException when URL is flagged unsafe")
    void shouldThrowWhenUrlIsUnsafe() {
        ShortenRequest request = new ShortenRequest();
        request.setOriginalUrl("http://malicious-site.com");

        when(urlMappingRepository.existsByShortCode(any())).thenReturn(false);
        when(pageFetcherService.fetchPageContent(any())).thenReturn(null);
        when(aiEnrichmentService.enrichUrl(any(), any())).thenReturn(Map.of(
                "safetyStatus", "UNSAFE",
                "safetyScore", 5,
                "safetyReason", "Malicious domain"
        ));

        assertThatThrownBy(() -> urlShortenerService.shortenUrl(request, "127.0.0.1"))
                .isInstanceOf(UrlUnsafeException.class)
                .hasMessageContaining("unsafe");
    }

    @Test
    @DisplayName("Should use AI suggested expiry when user does not provide one")
    void shouldUseAiSuggestedExpiry() {
        ShortenRequest request = new ShortenRequest();
        request.setOriginalUrl("https://github.com/subho30");

        Map<String, Object> aiData = Map.of(
                "safetyStatus", "SAFE",
                "safetyScore", 95,
                "safetyReason", "Safe",
                "title", "Title",
                "summary", "Summary",
                "category", "Technology",
                "tags", "Java",
                "suggestedExpiryDays", 7,
                "expiryReason", "News content"
        );

        when(urlMappingRepository.existsByShortCode(any())).thenReturn(false);
        when(pageFetcherService.fetchPageContent(any())).thenReturn("content");
        when(aiEnrichmentService.enrichUrl(any(), any())).thenReturn(aiData);
        when(urlMappingRepository.save(any())).thenAnswer(i -> {
            UrlMapping mapping = i.getArgument(0);
            if (mapping.getHitCount() == null) mapping.setHitCount(0L);
            return mapping;
        });

        ShortenResponse response = urlShortenerService.shortenUrl(request, "127.0.0.1");

        assertThat(response.getSuggestedExpiryDays()).isEqualTo(7);
        assertThat(response.getExpiresAt()).isAfter(LocalDateTime.now().plusDays(6));
        assertThat(response.getExpiresAt()).isBefore(LocalDateTime.now().plusDays(8));
    }

    // ── Resolve Short Code Tests ──

    @Test
    @DisplayName("Should return cached URL on cache hit")
    void shouldReturnCachedUrlOnCacheHit() {
        ShortenResponse cached = ShortenResponse.builder()
                .shortCode("abc123")
                .originalUrl("https://github.com/subho30")
                .shortUrl("http://localhost:8080/abc123")
                .hitCount(5L)
                .build();

        when(redisCacheService.getCachedUrl("abc123")).thenReturn(cached);

        String result = urlShortenerService.resolveShortCode("abc123");

        assertThat(result).isEqualTo("https://github.com/subho30");
        verify(urlMappingRepository, never()).findActiveByShortCode(any(), any());
        verify(urlMappingRepository, times(1)).incrementHitCount(any(), any());
    }

    @Test
    @DisplayName("Should hit database on cache miss")
    void shouldHitDatabaseOnCacheMiss() {
        UrlMapping urlMapping = mockUrlMapping("abc123");

        when(redisCacheService.getCachedUrl("abc123")).thenReturn(null);
        when(urlMappingRepository.findActiveByShortCode(eq("abc123"), any()))
                .thenReturn(Optional.of(urlMapping));

        String result = urlShortenerService.resolveShortCode("abc123");

        assertThat(result).isEqualTo("https://github.com/subho30");
        verify(urlMappingRepository, times(1)).findActiveByShortCode(any(), any());
        verify(redisCacheService, times(1)).cacheUrl(eq("abc123"), any());
    }

    @Test
    @DisplayName("Should throw UrlNotFoundException for unknown short code")
    void shouldThrowForUnknownShortCode() {
        when(redisCacheService.getCachedUrl(any())).thenReturn(null);
        when(urlMappingRepository.findActiveByShortCode(any(), any()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> urlShortenerService.resolveShortCode("unknown"))
                .isInstanceOf(UrlNotFoundException.class)
                .hasMessageContaining("unknown");
    }

    // ── Deactivate Tests ──

    @Test
    @DisplayName("Should deactivate link and evict cache")
    void shouldDeactivateLinkAndEvictCache() {
        UrlMapping urlMapping = mockUrlMapping("abc123");

        when(urlMappingRepository.findByShortCode("abc123"))
                .thenReturn(Optional.of(urlMapping));
        when(urlMappingRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        urlShortenerService.deactivateUrl("abc123");

        assertThat(urlMapping.getIsActive()).isFalse();
        verify(urlMappingRepository, times(1)).save(urlMapping);
        verify(redisCacheService, times(1)).evictCache("abc123");
    }

    @Test
    @DisplayName("Should throw UrlNotFoundException when deactivating unknown link")
    void shouldThrowWhenDeactivatingUnknownLink() {
        when(urlMappingRepository.findByShortCode("unknown"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> urlShortenerService.deactivateUrl("unknown"))
                .isInstanceOf(UrlNotFoundException.class);
    }

    // ── Short Code Generation Tests ──

    @Test
    @DisplayName("Should generate short code of correct length")
    void shouldGenerateShortCodeOfCorrectLength() {
        ShortenRequest request = new ShortenRequest();
        request.setOriginalUrl("https://github.com/subho30");

        when(urlMappingRepository.existsByShortCode(any())).thenReturn(false);
        when(pageFetcherService.fetchPageContent(any())).thenReturn("content");
        when(aiEnrichmentService.enrichUrl(any(), any())).thenReturn(mockSafeAiData());
        when(urlMappingRepository.save(any())).thenAnswer(i -> {
            UrlMapping mapping = i.getArgument(0);
            if (mapping.getHitCount() == null) mapping.setHitCount(0L);
            return mapping;
        });

        ShortenResponse response = urlShortenerService.shortenUrl(request, "127.0.0.1");

        assertThat(response.getShortCode()).hasSize(6);
        assertThat(response.getShortCode()).matches("[a-zA-Z0-9]{6}");
    }

    @Test
    @DisplayName("Should generate unique short codes on collision")
    void shouldRetryOnShortCodeCollision() {
        ShortenRequest request = new ShortenRequest();
        request.setOriginalUrl("https://github.com/subho30");

        when(urlMappingRepository.existsByShortCode(any()))
                .thenReturn(true)   // first attempt — collision
                .thenReturn(true)   // second attempt — collision
                .thenReturn(false); // third attempt — success
        when(pageFetcherService.fetchPageContent(any())).thenReturn("content");
        when(aiEnrichmentService.enrichUrl(any(), any())).thenReturn(mockSafeAiData());
        when(urlMappingRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        ShortenResponse response = urlShortenerService.shortenUrl(request, "127.0.0.1");

        assertThat(response.getShortCode()).isNotBlank();
        verify(urlMappingRepository, atLeast(3)).existsByShortCode(any());
    }
}