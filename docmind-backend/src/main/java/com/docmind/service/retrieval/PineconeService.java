package com.docmind.service.retrieval;

import com.docmind.config.PineconeProperties;
import com.docmind.dto.PineconeQueryRequest;
import com.docmind.dto.PineconeQueryResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class PineconeService {
    private final WebClient webClient;
    private final PineconeProperties pineconeProperties;

    public PineconeQueryResponse query(float[] vector, int topK) {
        String url = String.format("%s/query", pineconeProperties.indexHost());

        PineconeQueryRequest request = new PineconeQueryRequest(vector, topK, true);

        PineconeQueryResponse response = webClient.post()
            .uri(url)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + pineconeProperties.apiKey())
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .bodyValue(request)
            .retrieve()
            .onStatus(status -> !status.is2xxSuccessful(),
                clientResponse -> clientResponse.bodyToMono(String.class)
                    .flatMap(body -> {
                        log.error("Pinecone API error: {}", body);
                        return Mono.error(new RuntimeException("Pinecone API error: " + body));
                    }))
            .bodyToMono(PineconeQueryResponse.class)
            .block();

        if (response == null) {
            throw new RuntimeException("Failed to query Pinecone");
        }

        return response;
    }

    public boolean healthCheck() {
        try {
            float[] testVector = new float[768];
            testVector[0] = 1.0f;
            PineconeQueryResponse response = query(testVector, 1);
            return response != null;
        } catch (Exception e) {
            log.error("Pinecone health check failed: {}", e.getMessage());
            return false;
        }
    }
}
