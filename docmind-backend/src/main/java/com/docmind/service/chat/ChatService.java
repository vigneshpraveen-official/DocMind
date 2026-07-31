package com.docmind.service.chat;

import com.docmind.dto.ChatQueryResponse;
import com.docmind.dto.PineconeQueryResponse;
import com.docmind.dto.SourceReference;
import com.docmind.service.embedding.GeminiEmbeddingService;
import com.docmind.service.generation.GeminiGenerationService;
import com.docmind.service.generation.PromptBuilder;
import com.docmind.service.generation.RetrievedChunk;
import com.docmind.service.retrieval.PineconeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private static final int TOP_K = 5;

    private final GeminiEmbeddingService geminiEmbeddingService;
    private final PineconeService pineconeService;
    private final PromptBuilder promptBuilder;
    private final GeminiGenerationService geminiGenerationService;

    public ChatQueryResponse query(String question) {
        float[] queryVector = geminiEmbeddingService.embed(question, GeminiEmbeddingService.TASK_TYPE_QUERY);

        PineconeQueryResponse pineconeResponse = pineconeService.query(queryVector, TOP_K);
        List<PineconeQueryResponse.Match> matches = pineconeResponse.getMatches();

        if (matches == null || matches.isEmpty()) {
            log.info("No indexed chunks found for query: '{}'", question);
            return new ChatQueryResponse(
                    "I don't have information on that — no documents have been indexed yet.",
                    List.of()
            );
        }

        List<RetrievedChunk> chunks = matches.stream()
                .map(this::toRetrievedChunk)
                .toList();

        String prompt = promptBuilder.buildGroundedPrompt(question, chunks);
        String answer = geminiGenerationService.generate(prompt);

        List<SourceReference> sources = chunks.stream()
                .map(c -> new SourceReference(c.documentId(), c.filename(), c.pageNumber()))
                .distinct()
                .toList();

        log.info("Answered query '{}' using {} retrieved chunks, {} distinct sources",
                question, chunks.size(), sources.size());

        return new ChatQueryResponse(answer, sources);
    }

    private RetrievedChunk toRetrievedChunk(PineconeQueryResponse.Match match) {
        Map<String, Object> metadata = match.getMetadata();
        Long documentId = metadata.get("documentId") != null
                ? ((Number) metadata.get("documentId")).longValue() : null;
        Integer pageNumber = metadata.get("pageNumber") != null
                ? ((Number) metadata.get("pageNumber")).intValue() : null;
        String filename = (String) metadata.get("filename");
        String text = (String) metadata.get("text");

        return new RetrievedChunk(documentId, filename, pageNumber, text, match.getScore());
    }
}
