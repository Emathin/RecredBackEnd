package com.recred.backend.infrastructure.gemini;

import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

public interface GeminiTextClient {
    Optional<GeminiTextResponse> generate(String prompt);
    Optional<GeminiTextResponse> generateWithImage(String prompt, MultipartFile image);

}
