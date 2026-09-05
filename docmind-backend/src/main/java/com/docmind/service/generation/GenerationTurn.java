package com.docmind.service.generation;

import java.util.Map;

/** One turn of a Gemini generation call: either a final text answer, or a tool the model wants invoked. */
public record GenerationTurn(String text, String functionCallName, Map<String, Object> functionCallArgs,
                              String thoughtSignature) {

    public static GenerationTurn text(String text) {
        return new GenerationTurn(text, null, null, null);
    }

    public static GenerationTurn functionCall(String name, Map<String, Object> args, String thoughtSignature) {
        return new GenerationTurn(null, name, args, thoughtSignature);
    }

    public boolean isFunctionCall() {
        return functionCallName != null;
    }
}
