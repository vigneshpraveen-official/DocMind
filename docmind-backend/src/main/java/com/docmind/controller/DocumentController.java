package com.docmind.controller;

import com.docmind.dto.ChunkResponse;
import com.docmind.dto.DocumentSummaryResponse;
import com.docmind.dto.DocumentUploadResponse;
import com.docmind.entity.Document;
import com.docmind.repository.ChunkRepository;
import com.docmind.repository.DocumentRepository;
import com.docmind.service.document.DocumentIngestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentIngestionService documentIngestionService;
    private final DocumentRepository documentRepository;
    private final ChunkRepository chunkRepository;

    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    public ResponseEntity<DocumentUploadResponse> upload(@RequestParam("file") MultipartFile file) {
        DocumentUploadResponse response = documentIngestionService.ingest(file);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<DocumentSummaryResponse>> listDocuments() {
        List<DocumentSummaryResponse> documents = documentRepository.findAll().stream()
                .map(DocumentSummaryResponse::from)
                .toList();
        return ResponseEntity.ok(documents);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DocumentSummaryResponse> getDocument(@PathVariable Long id) {
        return documentRepository.findById(id)
                .map(document -> ResponseEntity.ok(DocumentSummaryResponse.from(document)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/chunks")
    public ResponseEntity<List<ChunkResponse>> getChunks(@PathVariable Long id) {
        List<ChunkResponse> chunks = chunkRepository.findByDocumentId(id).stream()
                .map(ChunkResponse::from)
                .toList();
        return ResponseEntity.ok(chunks);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleBadRequest(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(e.getMessage());
    }
}
