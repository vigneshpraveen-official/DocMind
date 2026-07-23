package com.docmind.repository;

import com.docmind.entity.Chunk;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChunkRepository extends JpaRepository<Chunk, Long> {
    List<Chunk> findByDocumentId(Long documentId);
    List<Chunk> findByPineconeVectorIdIn(List<String> pineconeVectorIds);
}
