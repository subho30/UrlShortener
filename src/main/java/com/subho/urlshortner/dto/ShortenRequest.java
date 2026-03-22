package com.subho.urlshortner.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class ShortenRequest {

    @NotBlank(message = "URL cannot be blank")
    @Pattern(
        regexp = "^(https?://)([\\w.-]+)(:[0-9]+)?(/.*)?$",
        message = "Invalid URL format. Must start with http:// or https://"
    )
    private String originalUrl;

    @Pattern(
            regexp = "^[a-zA-Z0-9-_]{6,12}$",
            message = "Custom alias must be 6-12 characters — letters, numbers, hyphens and underscores only"
    )
    private String customAlias;

    // Optional — how many days until expiry. Defaults to 30 if not provided
    private Integer expiryDays;
}