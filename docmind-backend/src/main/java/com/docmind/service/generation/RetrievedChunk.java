package com.docmind.service.generation;

public record RetrievedChunk(
        Long documentId,
        String filename,
        Integer pageNumber,
        String text,
        float score
) {
}
