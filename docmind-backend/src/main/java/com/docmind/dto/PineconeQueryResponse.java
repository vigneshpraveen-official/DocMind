package com.docmind.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PineconeQueryResponse {
    private List<Match> matches;
    private String namespace;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Match {
        private String id;
        private float score;
        private Map<String, Object> metadata;
    }
}
