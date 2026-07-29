package com.docmind.dto;

import com.docmind.entity.Chunk;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ChunkResponse {
    private Long id;
    private Integer pageNumber;
    private String chunkText;
    private String pineconeVectorId;

    public static ChunkResponse from(Chunk chunk) {
        return new ChunkResponse(
                chunk.getId(),
                chunk.getPageNumber(),
                chunk.getChunkText(),
                chunk.getPineconeVectorId()
        );
    }
}
