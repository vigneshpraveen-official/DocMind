package com.docmind.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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
        private Part[] parts;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Part {
        private String text;
    }

    public String getText() {
        if (candidates == null || candidates.length == 0) {
            return null;
        }
        Content content = candidates[0].getContent();
        if (content == null || content.getParts() == null || content.getParts().length == 0) {
            return null;
        }
        return content.getParts()[0].getText();
    }
}
