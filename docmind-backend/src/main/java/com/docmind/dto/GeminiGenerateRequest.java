package com.docmind.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GeminiGenerateRequest {
    private Content[] contents;
    private Tool[] tools;
    private GenerationConfig generationConfig;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Content {
        private String role;
        private Part[] parts;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Part {
        private String text;
        private FunctionCall functionCall;
        private FunctionResponse functionResponse;
        private String thoughtSignature;

        public Part(String text, FunctionCall functionCall, FunctionResponse functionResponse) {
            this(text, functionCall, functionResponse, null);
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FunctionCall {
        private String name;
        private Map<String, Object> args;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FunctionResponse {
        private String name;
        private Map<String, Object> response;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Tool {
        private List<Map<String, Object>> functionDeclarations;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GenerationConfig {
        private double temperature;
    }

    public static GeminiGenerateRequest of(String prompt, double temperature) {
        Part part = new Part(prompt, null, null, null);
        Content content = new Content("user", new Part[]{part});
        return new GeminiGenerateRequest(new Content[]{content}, null, new GenerationConfig(temperature));
    }
}
