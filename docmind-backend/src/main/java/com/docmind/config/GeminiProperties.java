package com.docmind.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "docmind.gemini")
public record GeminiProperties(
        String apiKey,
        String embeddingModel,
        int embeddingDimension,
        String generationModel,
        String baseUrl
) {
}
