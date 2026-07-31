package com.docmind.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GeminiGenerateRequest {
    private Content[] contents;
    private GenerationConfig generationConfig;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Content {
        private Part[] parts;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Part {
        private String text;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GenerationConfig {
        private double temperature;
    }

    public static GeminiGenerateRequest of(String prompt, double temperature) {
        Part part = new Part(prompt);
        Content content = new Content(new Part[]{part});
        return new GeminiGenerateRequest(new Content[]{content}, new GenerationConfig(temperature));
    }
}
