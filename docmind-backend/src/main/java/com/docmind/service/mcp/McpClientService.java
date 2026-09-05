package com.docmind.service.mcp;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * MCP *client* for the ChatService orchestrator: connects to this same application's own MCP
 * tool server (built Day 6, exposed at /mcp) so Gemini's function-calling can discover and invoke
 * tools like search_documents mid-conversation via the real JSON-RPC protocol, not a direct
 * in-process method call.
 *
 * The client connects lazily on first use rather than at startup — during context refresh the
 * embedded Tomcat server the client would connect to isn't accepting connections yet.
 */
@Slf4j
@Service
public class McpClientService {

    private final int serverPort;
    private volatile McpSyncClient client;

    public McpClientService(@Value("${server.port}") int serverPort) {
        this.serverPort = serverPort;
    }

    public List<Map<String, Object>> listToolDeclarationsForGemini() {
        return client().listTools().tools().stream()
                .map(tool -> Map.<String, Object>of(
                        "name", tool.name(),
                        "description", tool.description(),
                        "parameters", tool.inputSchema()))
                .toList();
    }

    public String callTool(String name, Map<String, Object> arguments) {
        McpSchema.CallToolRequest request = McpSchema.CallToolRequest.builder(name)
                .arguments(arguments)
                .build();
        McpSchema.CallToolResult result = client().callTool(request);

        String text = result.content().stream()
                .filter(McpSchema.TextContent.class::isInstance)
                .map(McpSchema.TextContent.class::cast)
                .map(McpSchema.TextContent::text)
                .findFirst()
                .orElse("");

        if (Boolean.TRUE.equals(result.isError())) {
            throw new RuntimeException("MCP tool call to '" + name + "' failed: " + text);
        }
        return text;
    }

    private McpSyncClient client() {
        McpSyncClient current = client;
        if (current == null) {
            synchronized (this) {
                current = client;
                if (current == null) {
                    current = connect();
                    client = current;
                }
            }
        }
        return current;
    }

    private McpSyncClient connect() {
        log.info("Connecting MCP client to self at http://localhost:{}/mcp", serverPort);
        HttpClientStreamableHttpTransport transport = HttpClientStreamableHttpTransport
                .builder("http://localhost:" + serverPort)
                .build();

        McpSyncClient syncClient = McpClient.sync(transport)
                .clientInfo(McpSchema.Implementation.builder("docmind-chat-orchestrator", "0.1.0").build())
                .build();
        syncClient.initialize();
        return syncClient;
    }
}
