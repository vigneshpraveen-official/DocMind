package com.docmind.eval;

import com.docmind.dto.PineconeQueryResponse;
import com.docmind.service.embedding.GeminiEmbeddingService;
import com.docmind.service.generation.GeminiGenerationService;
import com.docmind.service.generation.PromptBuilder;
import com.docmind.service.generation.RetrievedChunk;
import com.docmind.service.retrieval.PineconeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Day 5 eval harness. Only active under the "eval" profile — not part of normal app startup.
 * Runs every question in eval/questions.json through the SAME retrieved context twice: once
 * with the naive prompt (no grounding instruction) and once with the refined/grounded prompt,
 * so the comparison isolates the instruction's effect rather than retrieval variance. Writes
 * raw results to eval/results-raw.json (incrementally, so a crash mid-run doesn't lose earlier
 * answers) for manual scoring, then exits.
 *
 * Resumable: if eval/results-raw.json already has entries for some question IDs, those are
 * skipped (the free-tier generateContent quota is tight enough that re-running from scratch
 * after a rate-limit failure isn't practical).
 *
 * Run with: ./mvnw spring-boot:run -Dspring-boot.run.profiles=eval
 */
@Slf4j
@Component
@Profile("eval")
@RequiredArgsConstructor
public class EvalRunner implements CommandLineRunner {

    private static final int TOP_K = 5;
    private static final String QUESTIONS_PATH = "eval/questions.json";
    private static final String RESULTS_PATH = "eval/results-raw.json";

    // Free-tier generateContent quota is a handful of requests/minute; space calls out generously.
    private static final long DELAY_BETWEEN_GENERATE_CALLS_MS = 16_000;
    private static final long RATE_LIMIT_BACKOFF_MS = 70_000;
    private static final int MAX_ATTEMPTS = 6;

    private final GeminiEmbeddingService geminiEmbeddingService;
    private final PineconeService pineconeService;
    private final PromptBuilder promptBuilder;
    private final GeminiGenerationService geminiGenerationService;
    private final ConfigurableApplicationContext context;

    @Override
    public void run(String... args) throws Exception {
        ObjectMapper mapper = JsonMapper.builder().enable(SerializationFeature.INDENT_OUTPUT).build();

        List<Map<String, Object>> questions = mapper.readValue(new File(QUESTIONS_PATH), List.class);

        List<Map<String, Object>> results = new ArrayList<>();
        File resultsFile = new File(RESULTS_PATH);
        if (resultsFile.exists()) {
            results = new ArrayList<>(mapper.readValue(resultsFile, List.class));
        }
        Set<Object> alreadyDone = results.stream().map(r -> r.get("id")).collect(Collectors.toSet());

        log.info("=== Day 5 Eval: {} questions total, {} already done, {} remaining ===",
                questions.size(), alreadyDone.size(), questions.size() - alreadyDone.size());

        try {
            for (Map<String, Object> q : questions) {
                if (alreadyDone.contains(q.get("id"))) {
                    continue;
                }

                String question = (String) q.get("question");
                log.info("[{}] {}", q.get("id"), question);

                float[] queryVector = geminiEmbeddingService.embed(
                        question, GeminiEmbeddingService.TASK_TYPE_QUERY);
                PineconeQueryResponse pineconeResponse = pineconeService.query(queryVector, TOP_K);
                List<RetrievedChunk> chunks = pineconeResponse.getMatches().stream()
                        .map(this::toRetrievedChunk)
                        .toList();

                String naiveAnswer = generateWithRetry(promptBuilder.buildNaivePrompt(question, chunks));
                sleep(DELAY_BETWEEN_GENERATE_CALLS_MS);
                String groundedAnswer = generateWithRetry(promptBuilder.buildGroundedPrompt(question, chunks));
                sleep(DELAY_BETWEEN_GENERATE_CALLS_MS);

                Map<String, Object> result = new LinkedHashMap<>();
                result.put("id", q.get("id"));
                result.put("question", question);
                result.put("answerable", q.get("answerable"));
                result.put("expectedAnswer", q.get("expectedAnswer"));
                result.put("naiveAnswer", naiveAnswer);
                result.put("groundedAnswer", groundedAnswer);
                results.add(result);
            }
        } finally {
            results.sort((a, b) -> ((Comparable) a.get("id")).compareTo(b.get("id")));
            mapper.writeValue(resultsFile, results);
            log.info("=== Eval finished: wrote {}/{} results to {} ===",
                    results.size(), questions.size(), RESULTS_PATH);
        }

        int exitCode = SpringApplication.exit(context, () -> 0);
        System.exit(exitCode);
    }

    private String generateWithRetry(String prompt) throws InterruptedException {
        RuntimeException lastError = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                return geminiGenerationService.generate(prompt);
            } catch (RuntimeException e) {
                lastError = e;
                boolean rateLimited = e.getMessage() != null
                        && (e.getMessage().contains("RESOURCE_EXHAUSTED") || e.getMessage().contains("429"));
                if (rateLimited && attempt < MAX_ATTEMPTS) {
                    log.warn("Rate limited (attempt {}/{}), backing off {}ms",
                            attempt, MAX_ATTEMPTS, RATE_LIMIT_BACKOFF_MS);
                    sleep(RATE_LIMIT_BACKOFF_MS);
                } else if (!rateLimited) {
                    throw e;
                }
            }
        }
        throw lastError;
    }

    private void sleep(long millis) throws InterruptedException {
        Thread.sleep(millis);
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
