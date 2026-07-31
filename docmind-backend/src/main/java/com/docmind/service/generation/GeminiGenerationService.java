package com.docmind.service.generation;

import com.docmind.config.GeminiProperties;
import com.docmind.dto.GeminiGenerateRequest;
import com.docmind.dto.GeminiGenerateResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiGenerationService {

    private static final double GROUNDED_ANSWER_TEMPERATURE = 0.2;

    private final WebClient webClient;
    private final GeminiProperties geminiProperties;

    public String generate(String prompt) {
        String url = String.format(
            "%s/v1beta/models/%s:generateContent?key=%s",
            geminiProperties.baseUrl(),
            geminiProperties.generationModel(),
            geminiProperties.apiKey()
        );

        GeminiGenerateRequest request = GeminiGenerateRequest.of(prompt, GROUNDED_ANSWER_TEMPERATURE);

        GeminiGenerateResponse response = webClient.post()
            .uri(url)
            .bodyValue(request)
            .retrieve()
            .onStatus(status -> !status.is2xxSuccessful(),
                clientResponse -> clientResponse.bodyToMono(String.class)
                    .flatMap(body -> {
                        log.error("Gemini generation error: {}", body);
                        return Mono.error(new RuntimeException("Gemini generation error: " + body));
                    }))
            .bodyToMono(GeminiGenerateResponse.class)
            .block();

        String text = response != null ? response.getText() : null;
        if (text == null) {
            throw new RuntimeException("Gemini returned no generated text");
        }

        return text;
    }
}
