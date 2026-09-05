package com.docmind.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GeminiGenerateResponse {
    private Candidate[] candidates;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Candidate {
        private Content content;
    }

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
        private String thoughtSignature;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FunctionCall {
        private String name;
        private Map<String, Object> args;
    }

    public String getText() {
        Part part = getFirstPart();
        return part != null ? part.getText() : null;
    }

    public FunctionCall getFunctionCall() {
        Part part = getFirstPart();
        return part != null ? part.getFunctionCall() : null;
    }

    public String getThoughtSignature() {
        Part part = getFirstPart();
        return part != null ? part.getThoughtSignature() : null;
    }

    private Part getFirstPart() {
        if (candidates == null || candidates.length == 0) {
            return null;
        }
        Content content = candidates[0].getContent();
        if (content == null || content.getParts() == null || content.getParts().length == 0) {
            return null;
        }
        return content.getParts()[0];
    }
}
