package com.docmind.controller;

import com.docmind.service.embedding.GeminiEmbeddingService;
import com.docmind.service.retrieval.PineconeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import javax.sql.DataSource;
import java.sql.Connection;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
public class HealthController {
    private final GeminiEmbeddingService geminiEmbeddingService;
    private final PineconeService pineconeService;
    private final DataSource dataSource;

    @GetMapping
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "UP");

        Map<String, Boolean> connections = new LinkedHashMap<>();
        connections.put("postgres", checkPostgres());
        connections.put("gemini", geminiEmbeddingService.healthCheck());
        connections.put("pinecone", pineconeService.healthCheck());
        response.put("connections", connections);

        boolean allHealthy = connections.values().stream().allMatch(v -> v);
        response.put("allHealthy", allHealthy);

        return ResponseEntity.ok(response);
    }

    private boolean checkPostgres() {
        try {
            Connection conn = dataSource.getConnection();
            conn.close();
            return true;
        } catch (Exception e) {
            log.error("Postgres check failed: {}", e.getMessage());
            return false;
        }
    }
}
