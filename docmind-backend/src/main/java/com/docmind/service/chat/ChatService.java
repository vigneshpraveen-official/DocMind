package com.docmind.service.chat;

import com.docmind.dto.ChatQueryResponse;
import com.docmind.dto.GeminiGenerateRequest;
import com.docmind.dto.PineconeQueryResponse;
import com.docmind.dto.SourceReference;
import com.docmind.service.embedding.GeminiEmbeddingService;
import com.docmind.service.generation.GeminiGenerationService;
import com.docmind.service.generation.GenerationTurn;
import com.docmind.service.generation.PromptBuilder;
import com.docmind.service.generation.RetrievedChunk;
import com.docmind.service.mcp.McpClientService;
import com.docmind.service.retrieval.PineconeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
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
    private final McpClientService mcpClientService;

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
        String answer = generateWithToolSupport(prompt);

        List<SourceReference> sources = chunks.stream()
                .map(c -> new SourceReference(c.documentId(), c.filename(), c.pageNumber()))
                .distinct()
                .toList();

        log.info("Answered query '{}' using {} retrieved chunks, {} distinct sources",
                question, chunks.size(), sources.size());

        return new ChatQueryResponse(answer, sources);
    }

    /**
     * Lets Gemini answer from the grounded prompt directly, or — via MCP function-calling —
     * invoke the search_documents tool (Day 6) for structured questions the retrieved chunks
     * can't answer (e.g. "how many documents have been uploaded"). Single-hop: at most one
     * tool call per query, matching the master doc's Day 7 scope.
     */
    private String generateWithToolSupport(String prompt) {
        List<Map<String, Object>> toolDeclarations = mcpClientService.listToolDeclarationsForGemini();

        List<GeminiGenerateRequest.Content> contents = new ArrayList<>();
        contents.add(new GeminiGenerateRequest.Content("user",
                new GeminiGenerateRequest.Part[]{new GeminiGenerateRequest.Part(prompt, null, null)}));

        GenerationTurn turn = geminiGenerationService.generate(contents, toolDeclarations);
        if (!turn.isFunctionCall()) {
            return turn.text();
        }

        log.info("Gemini requested MCP tool call: {} with args {}", turn.functionCallName(), turn.functionCallArgs());
        String toolResultJson = mcpClientService.callTool(turn.functionCallName(), turn.functionCallArgs());

        contents.add(new GeminiGenerateRequest.Content("model",
                new GeminiGenerateRequest.Part[]{new GeminiGenerateRequest.Part(
                        null,
                        new GeminiGenerateRequest.FunctionCall(turn.functionCallName(), turn.functionCallArgs()),
                        null,
                        turn.thoughtSignature())}));
        contents.add(new GeminiGenerateRequest.Content("user",
                new GeminiGenerateRequest.Part[]{new GeminiGenerateRequest.Part(
                        null,
                        null,
                        new GeminiGenerateRequest.FunctionResponse(
                                turn.functionCallName(), Map.of("result", toolResultJson)))}));

        GenerationTurn finalTurn = geminiGenerationService.generate(contents, List.of());
        return finalTurn.text();
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
