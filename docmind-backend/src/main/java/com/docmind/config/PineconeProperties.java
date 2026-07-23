package com.docmind.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "docmind.pinecone")
public record PineconeProperties(
        String apiKey,
        String indexHost
) {
}
