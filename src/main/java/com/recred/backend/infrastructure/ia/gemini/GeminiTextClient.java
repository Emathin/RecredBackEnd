package com.recred.backend.infrastructure.ia.gemini;

import java.util.Optional;

public interface GeminiTextClient {
    Optional<GeminiTextResponse> generate(String prompt);
}
