package com.docmind.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GeminiEmbedRequest {
    private Content content;
    private String taskType;
    private Integer outputDimensionality;

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

    public static GeminiEmbedRequest of(String text, String taskType, int outputDimensionality) {
        Part part = new Part(text);
        Content content = new Content(new Part[]{part});
        return new GeminiEmbedRequest(content, taskType, outputDimensionality);
    }
}
