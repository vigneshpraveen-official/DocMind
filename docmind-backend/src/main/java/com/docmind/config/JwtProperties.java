package com.docmind.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "docmind.jwt")
public record JwtProperties(
        String secret,
        long expirationMs
) {
}
