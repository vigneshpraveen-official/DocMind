package com.docmind.service.document;

import com.docmind.dto.DocumentUploadResponse;
import com.docmind.dto.PineconeVector;
import com.docmind.entity.Chunk;
import com.docmind.entity.Document;
import com.docmind.entity.DocumentStatus;
import com.docmind.repository.ChunkRepository;
import com.docmind.repository.DocumentRepository;
import com.docmind.service.embedding.GeminiEmbeddingService;
import com.docmind.service.retrieval.PineconeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentIngestionService {

    private final DocumentExtractionService documentExtractionService;
    private final ChunkingService chunkingService;
    private final DocumentRepository documentRepository;
    private final ChunkRepository chunkRepository;
    private final GeminiEmbeddingService geminiEmbeddingService;
    private final PineconeService pineconeService;

    public DocumentUploadResponse ingest(MultipartFile file) {
        validateFile(file);

        Document document = Document.builder()
                .filename(file.getOriginalFilename())
                .status(DocumentStatus.PROCESSING)
                .build();
        document = documentRepository.save(document);

        List<Chunk> chunks;
        try {
            chunks = extractAndChunk(file, document);
        } catch (IOException e) {
            log.error("Failed to extract document '{}': {}", document.getFilename(), e.getMessage());
            markFailed(document);
            throw new RuntimeException("Failed to process document: " + e.getMessage(), e);
        }

        if (chunks.isEmpty()) {
            markFailed(document);
            return new DocumentUploadResponse(
                    document.getId(), document.getFilename(), document.getStatus().name(),
                    "No extractable text found in document (scanned image PDFs are not yet supported)", 0
            );
        }

        chunkRepository.saveAll(chunks);
        log.info("Chunked document '{}' (id={}): {} chunks", document.getFilename(), document.getId(), chunks.size());

        try {
            embedAndUpsert(document, chunks);
        } catch (Exception e) {
            log.error("Failed to embed/upsert document '{}': {}", document.getFilename(), e.getMessage());
            markFailed(document);
            throw new RuntimeException("Chunks were saved, but embedding failed: " + e.getMessage(), e);
        }

        document.setStatus(DocumentStatus.PROCESSED);
        documentRepository.save(document);

        return new DocumentUploadResponse(
                document.getId(), document.getFilename(), document.getStatus().name(),
                "Document processed: extracted, chunked, embedded, and indexed.", chunks.size()
        );
    }

    private List<Chunk> extractAndChunk(MultipartFile file, Document document) throws IOException {
        Map<Integer, String> pageTexts = documentExtractionService.extractTextFromPDF(file);

        List<Chunk> chunks = new ArrayList<>();
        for (Map.Entry<Integer, String> entry : pageTexts.entrySet()) {
            int pageNumber = entry.getKey();
            List<String> pageChunks = chunkingService.chunkText(entry.getValue());

            for (String chunkText : pageChunks) {
                if (chunkText.isBlank()) {
                    continue;
                }
                chunks.add(Chunk.builder()
                        .document(document)
                        .pineconeVectorId(UUID.randomUUID().toString())
                        .chunkText(chunkText)
                        .pageNumber(pageNumber)
                        .build());
            }
        }
        return chunks;
    }

    private void embedAndUpsert(Document document, List<Chunk> chunks) {
        List<PineconeVector> vectors = new ArrayList<>(chunks.size());

        for (Chunk chunk : chunks) {
            float[] embedding = geminiEmbeddingService.embed(
                    chunk.getChunkText(), GeminiEmbeddingService.TASK_TYPE_DOCUMENT);

            Map<String, Object> metadata = new LinkedHashMap<>();
            metadata.put("documentId", document.getId());
            metadata.put("chunkId", chunk.getId());
            metadata.put("filename", document.getFilename());
            metadata.put("pageNumber", chunk.getPageNumber());
            metadata.put("text", chunk.getChunkText());

            vectors.add(new PineconeVector(chunk.getPineconeVectorId(), embedding, metadata));
        }

        int upserted = pineconeService.upsert(vectors);
        log.info("Upserted {} vectors to Pinecone for document '{}' (id={})",
                upserted, document.getFilename(), document.getId());
    }

    private void markFailed(Document document) {
        document.setStatus(DocumentStatus.FAILED);
        documentRepository.save(document);
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file is empty");
        }
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".pdf")) {
            throw new IllegalArgumentException("Only PDF files are supported");
        }
    }
}
