package com.docmind.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PineconeUpsertRequest {
    private List<PineconeVector> vectors;
    private String namespace;

    public PineconeUpsertRequest(List<PineconeVector> vectors) {
        this.vectors = vectors;
        this.namespace = "";
    }
}
