package com.recred.backend.infrastructure.gemini;

import com.recred.backend.config.GeminiProperties;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class GoogleGeminiTextClientTest {

    @Test
    void noGenerarTextoCuandoGeminiNoEstaConfigurado() {
        GoogleGeminiTextClient client = new GoogleGeminiTextClient(new GeminiProperties(
                "",
                "gemini-test",
                true
        ));

        Optional<GeminiTextResponse> response = client.generate("prompt");

        assertTrue(response.isEmpty());
    }
}
