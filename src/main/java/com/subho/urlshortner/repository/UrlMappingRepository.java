package com.subho.urlshortner.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.subho.urlshortner.model.UrlMapping;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface UrlMappingRepository extends JpaRepository<UrlMapping, Long> {

    // Find by short code
    Optional<UrlMapping> findByShortCode(String shortCode);

    // Check if short code already exists
    boolean existsByShortCode(String shortCode);

    // Find only active and non-expired links
    @Query("SELECT u FROM UrlMapping u WHERE u.shortCode = :shortCode " +
           "AND u.isActive = true " +
           "AND (u.expiresAt IS NULL OR u.expiresAt > :now)")
    Optional<UrlMapping> findActiveByShortCode(@Param("shortCode") String shortCode,
                                               @Param("now") LocalDateTime now);

    // Increment hit count and update last accessed
    @Modifying
    @Query("UPDATE UrlMapping u SET u.hitCount = u.hitCount + 1, " +
           "u.lastAccessedAt = :now WHERE u.shortCode = :shortCode")
    void incrementHitCount(@Param("shortCode") String shortCode,
                           @Param("now") LocalDateTime now);
}