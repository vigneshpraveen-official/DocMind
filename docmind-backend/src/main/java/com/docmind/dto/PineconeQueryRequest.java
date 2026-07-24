package com.docmind.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PineconeQueryRequest {
    private float[] vector;
    private int topK;
    private boolean includeMetadata;
    private String namespace;

    public PineconeQueryRequest(float[] vector, int topK, boolean includeMetadata) {
        this.vector = vector;
        this.topK = topK;
        this.includeMetadata = includeMetadata;
        this.namespace = ""; // default namespace
    }
}
