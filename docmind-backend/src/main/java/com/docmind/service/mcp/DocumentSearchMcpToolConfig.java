package com.docmind.service.mcp;

import com.docmind.entity.Document;
import com.docmind.entity.DocumentStatus;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

/**
 * Exposes the "search_documents" MCP tool: structured lookup over ingested documents by
 * filename keyword, upload date, and status — a complement to Pinecone's semantic search,
 * for questions like "list documents uploaded this month" that similarity search can't answer.
 */
@Configuration
@RequiredArgsConstructor
public class DocumentSearchMcpToolConfig {

    private final DocumentSearchService documentSearchService;
    private final JsonMapper jsonMapper;

    @Bean
    public List<McpServerFeatures.SyncToolSpecification> documentSearchToolSpecifications() {
        Map<String, Object> inputSchema = Map.of(
                "type", "object",
                "properties", Map.of(
                        "query", Map.of(
                                "type", "string",
                                "description", "Case-insensitive substring to match against the document filename"),
                        "uploadedAfter", Map.of(
                                "type", "string",
                                "format", "date",
                                "description", "Only include documents uploaded after this date (ISO-8601, e.g. 2026-08-01)"),
                        "status", Map.of(
                                "type", "string",
                                "enum", List.of("PROCESSING", "PROCESSED", "FAILED"),
                                "description", "Only include documents in this processing status")),
                "required", List.of());

        McpSchema.Tool tool = McpSchema.Tool.builder("search_documents", inputSchema)
                .description("Search ingested documents by filename keyword, upload date, and/or "
                        + "processing status. Use for structured lookups (e.g. \"documents uploaded "
                        + "this month\") rather than semantic content search.")
                .build();

        McpServerFeatures.SyncToolSpecification specification = McpServerFeatures.SyncToolSpecification.builder()
                .tool(tool)
                .callHandler((exchange, request) -> handleSearch(request))
                .build();

        return List.of(specification);
    }

    private McpSchema.CallToolResult handleSearch(McpSchema.CallToolRequest request) {
        Map<String, Object> arguments = request.arguments();
        try {
            String query = (String) arguments.get("query");
            LocalDate uploadedAfter = parseDate((String) arguments.get("uploadedAfter"));
            DocumentStatus status = parseStatus((String) arguments.get("status"));

            List<Document> results = documentSearchService.search(query, uploadedAfter, status);
            List<Map<String, Object>> summaries = results.stream()
                    .map(this::toSummary)
                    .toList();

            return McpSchema.CallToolResult.builder()
                    .addTextContent(jsonMapper.writeValueAsString(summaries))
                    .build();
        } catch (IllegalArgumentException | DateTimeParseException e) {
            return McpSchema.CallToolResult.builder()
                    .isError(true)
                    .addTextContent("Invalid arguments: " + e.getMessage())
                    .build();
        }
    }

    private Map<String, Object> toSummary(Document document) {
        return Map.of(
                "id", document.getId(),
                "filename", document.getFilename(),
                "status", document.getStatus().name(),
                "uploadedAt", document.getUploadedAt() == null ? "" : document.getUploadedAt().toString());
    }

    private LocalDate parseDate(String raw) {
        return raw == null || raw.isBlank() ? null : LocalDate.parse(raw);
    }

    private DocumentStatus parseStatus(String raw) {
        return raw == null || raw.isBlank() ? null : DocumentStatus.valueOf(raw.toUpperCase());
    }
}
