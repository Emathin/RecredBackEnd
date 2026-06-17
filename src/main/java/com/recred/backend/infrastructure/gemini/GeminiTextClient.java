package com.recred.backend.infrastructure.gemini;

import java.util.Optional;

public interface GeminiTextClient {
    Optional<GeminiTextResponse> generate(String prompt);
    Optional<GeminiTextResponse> generateWithFile(String prompt, 
                                                  byte[] fileContent, 
                                                  String mimeType);
}
