package com.docmind.service.generation;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PromptBuilder {

    private static final String SYSTEM_INSTRUCTION = """
            Answer the user's question using ONLY the context below.
            If the context does not contain the answer but a search_documents tool is available,
            call it to look up the information before giving up.
            If neither the context nor the tool has the answer, say "I don't have information on that."
            Do not use outside knowledge. Cite which section supports your answer.
            """;

    public String buildGroundedPrompt(String question, List<RetrievedChunk> chunks) {
        StringBuilder context = new StringBuilder();
        for (RetrievedChunk chunk : chunks) {
            context.append("[Source: ").append(chunk.filename())
                    .append(", page ").append(chunk.pageNumber()).append("]\n")
                    .append(chunk.text())
                    .append("\n---\n");
        }

        return SYSTEM_INSTRUCTION
                + "\n\nContext:\n" + context
                + "\n\nQuestion: " + question;
    }

    /**
     * No grounding instruction — same retrieved context, but nothing telling the model to
     * stick to it or to admit when it doesn't know. Exists only for the Day 5 eval comparison
     * against {@link #buildGroundedPrompt}, to isolate the instruction's effect on hallucination.
     */
    public String buildNaivePrompt(String question, List<RetrievedChunk> chunks) {
        StringBuilder context = new StringBuilder();
        for (RetrievedChunk chunk : chunks) {
            context.append(chunk.text()).append("\n---\n");
        }

        return "Context:\n" + context + "\n\nQuestion: " + question;
    }
}
