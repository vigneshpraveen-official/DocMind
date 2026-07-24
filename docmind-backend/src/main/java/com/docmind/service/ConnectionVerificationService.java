package com.docmind.service;

import com.docmind.service.embedding.GeminiEmbeddingService;
import com.docmind.service.retrieval.PineconeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import javax.sql.DataSource;
import java.sql.Connection;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConnectionVerificationService {
    private final GeminiEmbeddingService geminiEmbeddingService;
    private final PineconeService pineconeService;
    private final DataSource dataSource;

    @EventListener(ApplicationReadyEvent.class)
    public void verifyConnections() {
        log.info("=== Starting Day 1 Connection Verification ===");

        boolean postgresOk = verifyPostgres();
        boolean geminiOk = verifyGemini();
        boolean pineconeOk = verifyPinecone();

        log.info("=== Connection Verification Results ===");
        log.info("Postgres: {}", postgresOk ? "✅ OK" : "❌ FAILED");
        log.info("Gemini: {}", geminiOk ? "✅ OK" : "❌ FAILED");
        log.info("Pinecone: {}", pineconeOk ? "✅ OK" : "❌ FAILED");
        log.info("====================================");

        if (!postgresOk || !geminiOk || !pineconeOk) {
            log.warn("⚠️  Some connections failed. Check your credentials and network.");
        } else {
            log.info("✅ All connections verified successfully!");
        }
    }

    private boolean verifyPostgres() {
        try {
            Connection conn = dataSource.getConnection();
            conn.close();
            log.info("✅ Postgres connection successful");
            return true;
        } catch (Exception e) {
            log.error("❌ Postgres connection failed: {}", e.getMessage());
            return false;
        }
    }

    private boolean verifyGemini() {
        boolean result = geminiEmbeddingService.healthCheck();
        if (result) {
            log.info("✅ Gemini API connection successful");
        } else {
            log.error("❌ Gemini API connection failed");
        }
        return result;
    }

    private boolean verifyPinecone() {
        boolean result = pineconeService.healthCheck();
        if (result) {
            log.info("✅ Pinecone connection successful");
        } else {
            log.error("❌ Pinecone connection failed");
        }
        return result;
    }
}
