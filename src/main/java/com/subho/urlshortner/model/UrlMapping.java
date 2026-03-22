package com.subho.urlshortner.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "url_mappings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UrlMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "short_code", unique = true, nullable = false, length = 10)
    private String shortCode;

    @Column(name = "original_url", nullable = false, columnDefinition = "TEXT")
    private String originalUrl;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "hit_count", nullable = false)
    private Long hitCount = 0L;

    @Column(name = "last_accessed_at")
    private LocalDateTime lastAccessedAt;

    @Column(name = "created_by_ip", length = 45)
    private String createdByIp;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    @Column(name = "title", length = 255)
    private String title;

    @Column(name = "category", length = 50)
    private String category;

    @Column(name = "tags", length = 200)
    private String tags;

    @Column(name = "safety_status", length = 10)
    private String safetyStatus;

    @Column(name = "safety_score")
    private Integer safetyScore;

    @Column(name = "suggested_expiry_days")
    private Integer suggestedExpiryDays;

    @Column(name = "expiry_reason", length = 255)
    private String expiryReason;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        if (this.hitCount == null) this.hitCount = 0L;
        if (this.isActive == null) this.isActive = true;
    }
}