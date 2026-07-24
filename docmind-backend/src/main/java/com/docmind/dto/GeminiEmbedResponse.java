package com.docmind.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GeminiEmbedResponse {
    private Embedding embedding;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Embedding {
        private float[] values;
    }

    public float[] getValues() {
        return embedding != null ? embedding.values : new float[0];
    }
}
