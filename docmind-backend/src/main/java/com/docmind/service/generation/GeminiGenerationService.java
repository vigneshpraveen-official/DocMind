package com.docmind.service.generation;

import com.docmind.config.GeminiProperties;
import com.docmind.dto.GeminiGenerateRequest;
import com.docmind.dto.GeminiGenerateResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiGenerationService {

    private static final double GROUNDED_ANSWER_TEMPERATURE = 0.2;

    private final WebClient webClient;
    private final GeminiProperties geminiProperties;

    public String generate(String prompt) {
        GeminiGenerateRequest request = GeminiGenerateRequest.of(prompt, GROUNDED_ANSWER_TEMPERATURE);
        GeminiGenerateResponse response = callGemini(request);

        String text = response.getText();
        if (text == null) {
            throw new RuntimeException("Gemini returned no generated text");
        }
        return text;
    }

    /** Tool-aware generation: lets Gemini either answer directly or request a tool invocation. */
    public GenerationTurn generate(List<GeminiGenerateRequest.Content> contents, List<Map<String, Object>> toolDeclarations) {
        GeminiGenerateRequest.Tool[] tools = toolDeclarations == null || toolDeclarations.isEmpty()
                ? null
                : new GeminiGenerateRequest.Tool[]{new GeminiGenerateRequest.Tool(toolDeclarations)};

        GeminiGenerateRequest request = new GeminiGenerateRequest(
                contents.toArray(new GeminiGenerateRequest.Content[0]),
                tools,
                new GeminiGenerateRequest.GenerationConfig(GROUNDED_ANSWER_TEMPERATURE));

        GeminiGenerateResponse response = callGemini(request);

        GeminiGenerateResponse.FunctionCall functionCall = response.getFunctionCall();
        if (functionCall != null) {
            return GenerationTurn.functionCall(functionCall.getName(), functionCall.getArgs(), response.getThoughtSignature());
        }

        String text = response.getText();
        if (text == null) {
            throw new RuntimeException("Gemini returned neither text nor a function call");
        }
        return GenerationTurn.text(text);
    }

    private GeminiGenerateResponse callGemini(GeminiGenerateRequest request) {
        String url = String.format(
            "%s/v1beta/models/%s:generateContent?key=%s",
            geminiProperties.baseUrl(),
            geminiProperties.generationModel(),
            geminiProperties.apiKey()
        );

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

        if (response == null) {
            throw new RuntimeException("Gemini returned an empty response");
        }
        return response;
    }
}
