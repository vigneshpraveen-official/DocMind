package com.docmind.service.embedding;

import com.docmind.config.GeminiProperties;
import com.docmind.dto.GeminiEmbedRequest;
import com.docmind.dto.GeminiEmbedResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiEmbeddingService {
    private final WebClient webClient;
    private final GeminiProperties geminiProperties;

    public static final String TASK_TYPE_DOCUMENT = "RETRIEVAL_DOCUMENT";
    public static final String TASK_TYPE_QUERY = "RETRIEVAL_QUERY";

    public float[] embed(String text) {
        return embed(text, TASK_TYPE_DOCUMENT);
    }

    public float[] embed(String text, String taskType) {
        String url = String.format(
            "%s/v1beta/models/%s:embedContent?key=%s",
            geminiProperties.baseUrl(),
            geminiProperties.embeddingModel(),
            geminiProperties.apiKey()
        );

        GeminiEmbedRequest request = GeminiEmbedRequest.of(text, taskType, geminiProperties.embeddingDimension());

        GeminiEmbedResponse response = webClient.post()
            .uri(url)
            .bodyValue(request)
            .retrieve()
            .onStatus(status -> !status.is2xxSuccessful(),
                clientResponse -> clientResponse.bodyToMono(String.class)
                    .flatMap(body -> {
                        log.error("Gemini API error: {}", body);
                        return Mono.error(new RuntimeException("Gemini API error: " + body));
                    }))
            .bodyToMono(GeminiEmbedResponse.class)
            .block();

        if (response == null || response.getValues() == null) {
            throw new RuntimeException("Failed to get embedding from Gemini");
        }

        return response.getValues();
    }

    public boolean healthCheck() {
        try {
            float[] embedding = embed("health check");
            return embedding.length == geminiProperties.embeddingDimension();
        } catch (Exception e) {
            log.error("Gemini health check failed: {}", e.getMessage());
            return false;
        }
    }
}
