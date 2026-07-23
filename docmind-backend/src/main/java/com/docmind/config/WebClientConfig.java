package com.docmind.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient geminiWebClient(GeminiProperties geminiProperties) {
        return WebClient.builder()
                .baseUrl(geminiProperties.baseUrl())
                .build();
    }

    @Bean
    public WebClient pineconeWebClient(PineconeProperties pineconeProperties) {
        return WebClient.builder()
                .baseUrl("https://" + pineconeProperties.indexHost())
                .defaultHeader("Api-Key", pineconeProperties.apiKey())
                .build();
    }
}
