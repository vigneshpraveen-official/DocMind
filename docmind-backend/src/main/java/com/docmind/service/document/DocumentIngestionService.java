package com.docmind.service.document;

import com.docmind.dto.DocumentUploadResponse;
import com.docmind.entity.Chunk;
import com.docmind.entity.Document;
import com.docmind.entity.DocumentStatus;
import com.docmind.repository.ChunkRepository;
import com.docmind.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
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

    @Transactional
    public DocumentUploadResponse ingest(MultipartFile file) {
        validateFile(file);

        Document document = Document.builder()
                .filename(file.getOriginalFilename())
                .status(DocumentStatus.PROCESSING)
                .build();
        document = documentRepository.save(document);

        try {
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

            if (chunks.isEmpty()) {
                document.setStatus(DocumentStatus.FAILED);
                documentRepository.save(document);
                return new DocumentUploadResponse(
                        document.getId(), document.getFilename(), document.getStatus().name(),
                        "No extractable text found in document (scanned image PDFs are not yet supported)", 0
                );
            }

            chunkRepository.saveAll(chunks);

            log.info("Ingested document '{}' (id={}): {} pages, {} chunks",
                    document.getFilename(), document.getId(), pageTexts.size(), chunks.size());

            return new DocumentUploadResponse(
                    document.getId(), document.getFilename(), document.getStatus().name(),
                    "Document extracted and chunked successfully. Awaiting embedding.", chunks.size()
            );
        } catch (IOException e) {
            log.error("Failed to ingest document '{}': {}", document.getFilename(), e.getMessage());
            document.setStatus(DocumentStatus.FAILED);
            documentRepository.save(document);
            throw new RuntimeException("Failed to process document: " + e.getMessage(), e);
        }
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
