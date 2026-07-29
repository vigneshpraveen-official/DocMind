package com.docmind.service.document;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class ChunkingService {

    private static final int DEFAULT_CHUNK_SIZE_WORDS = 500;
    private static final int DEFAULT_OVERLAP_WORDS = 50;

    public List<String> chunkText(String text) {
        return chunkText(text, DEFAULT_CHUNK_SIZE_WORDS, DEFAULT_OVERLAP_WORDS);
    }

    /**
     * Word-boundary sliding window chunking. Splits on whitespace instead of raw
     * character offsets so chunks never cut a word in half at the boundary.
     */
    public List<String> chunkText(String text, int chunkSizeWords, int overlapWords) {
        List<String> chunks = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return chunks;
        }

        String[] words = text.trim().split("\\s+");
        if (words.length == 0) {
            return chunks;
        }

        int start = 0;
        int step = chunkSizeWords - overlapWords;
        while (start < words.length) {
            int end = Math.min(start + chunkSizeWords, words.length);
            String chunk = String.join(" ", java.util.Arrays.copyOfRange(words, start, end));
            chunks.add(chunk);

            if (end == words.length) {
                break;
            }
            start += step;
        }

        log.debug("Split text ({} words) into {} chunks", words.length, chunks.size());
        return chunks;
    }
}
