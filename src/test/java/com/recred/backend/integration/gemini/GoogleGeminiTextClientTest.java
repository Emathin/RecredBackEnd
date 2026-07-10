package com.recred.backend.integration.gemini;

import com.google.genai.Client;
import com.recred.backend.config.GeminiProperties;
import com.recred.backend.infrastructure.gemini.GeminiTextResponse;
import com.recred.backend.infrastructure.gemini.GoogleGeminiTextClient;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GoogleGeminiTextClientTest {

    private static final String MODEL = "gemini-test";
    private static final String VISION_MODEL = MODEL;
    private static final String API_KEY = "test-api-key";
    private static final String BASE_PATH = "/v1beta/models/" + MODEL + ":generateContent";
    private static final String VISION_PATH = "/v1beta/models/" + VISION_MODEL + ":generateContent";
    
    private static final String VALID_JSON_RESPONSE = """
            {
              "candidates": [
                {
                  "content": {
                    "parts": [
                      {
                        "text": "{\\"resumen\\":\\"Todo bien\\"}"
                      }
                    ],
                    "role": "model"
                  },
                  "finishReason": "STOP"
                }
              ]
            }
            """;
            
    private static final String BLANK_TEXT_RESPONSE = """
            {
              "candidates": [
                {
                  "content": {
                    "parts": [
                      {
                        "text": "   "
                      }
                    ],
                    "role": "model"
                  },
                  "finishReason": "STOP"
                }
              ]
            }
            """;
            
    private static final String FAILED_RESPONSE = """
            {
              "error": {
                "code": 400,
                "message": "Solicitud invalida",
                "status": "INVALID_ARGUMENT"
              }
            }
            """;
            
    private static final String VALID_VISION_JSON_RESPONSE = """
            {
              "candidates": [
                {
                  "content": {
                    "parts": [
                      {
                        "text": "{\\"nombre\\":\\"Coca Cola\\"}"
                      }
                    ],
                    "role": "model"
                  },
                  "finishReason": "STOP"
                }
              ]
            }
            """;

    private HttpServer server;

    @AfterEach
    void tearDown() {
        Client.setDefaultBaseUrls(Optional.empty(), Optional.empty());
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void shouldNotGenerateTextWhenGeminiIsNotConfigured() {
        GoogleGeminiTextClient client = givenUnconfiguredClient();

        Optional<GeminiTextResponse> response = whenGenerateText(client);

        thenResponseIsEmpty(response);
    }

    @Test
    void shouldGenerateTextWithValidGeminiResponse() throws IOException {
        givenGeminiServerReturning(BASE_PATH, 200, VALID_JSON_RESPONSE);
        GoogleGeminiTextClient client = givenConfiguredClient();

        Optional<GeminiTextResponse> response = whenGenerateText(client);

        thenResponseHasExpectedData(response, "{\"resumen\":\"Todo bien\"}", MODEL);
    }

    @Test
    void shouldReturnEmptyWhenGeminiReturnsBlankText() throws IOException {
        givenGeminiServerReturning(BASE_PATH, 200, BLANK_TEXT_RESPONSE);
        GoogleGeminiTextClient client = givenConfiguredClient();

        Optional<GeminiTextResponse> response = whenGenerateText(client);

        thenResponseIsEmpty(response);
    }

    @Test
    void shouldReturnEmptyWhenGeminiFails() throws IOException {
        givenGeminiServerReturning(BASE_PATH, 400, FAILED_RESPONSE);
        GoogleGeminiTextClient client = givenConfiguredClient();

        Optional<GeminiTextResponse> response = whenGenerateText(client);

        thenResponseIsEmpty(response);
    }

    @Test
    void shouldNotGenerateTextWithImageWhenGeminiIsNotConfigured() {
        GoogleGeminiTextClient client = givenUnconfiguredClient();

        Optional<GeminiTextResponse> response = whenGenerateTextWithDummyImage(client);

        thenResponseIsEmpty(response);
    }

    @Test
    void shouldGenerateTextWithImageWithValidResponse() throws IOException {
        givenGeminiServerReturning(VISION_PATH, 200, VALID_VISION_JSON_RESPONSE);
        GoogleGeminiTextClient client = givenConfiguredClient();

        Optional<GeminiTextResponse> response = whenGenerateTextWithDummyImage(client);

        thenResponseHasExpectedData(response, "{\"nombre\":\"Coca Cola\"}", VISION_MODEL);
    }

    @Test
    void shouldReturnEmptyWhenGeminiFailsWithImage() throws IOException {
        givenGeminiServerReturning(VISION_PATH, 400, "Error");
        GoogleGeminiTextClient client = givenConfiguredClient();

        Optional<GeminiTextResponse> response = whenGenerateTextWithDummyImage(client);

        thenResponseIsEmpty(response);
    }

    private GoogleGeminiTextClient givenConfiguredClient() {
        return new GoogleGeminiTextClient(new GeminiProperties(API_KEY, MODEL, true));
    }

    private GoogleGeminiTextClient givenUnconfiguredClient() {
        return new GoogleGeminiTextClient(new GeminiProperties("", MODEL, true));
    }

    private void givenGeminiServerReturning(String path, int status, String body) throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext(path, exchange -> respond(exchange, status, body));
        server.start();
        Client.setDefaultBaseUrls(
                Optional.of("http://localhost:" + server.getAddress().getPort()),
                Optional.empty()
        );
    }

    private Optional<GeminiTextResponse> whenGenerateText(GoogleGeminiTextClient client) {
        return client.generate("dummy-prompt");
    }

    private Optional<GeminiTextResponse> whenGenerateTextWithDummyImage(GoogleGeminiTextClient client) {
        try {
            MockMultipartFile file = new MockMultipartFile(
                    "image", "dummy.jpg", "image/jpeg", "dummy-content".getBytes());
            return client.generateWithFile("dummy-prompt", file.getBytes(), file.getContentType());
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate dummy file bytes", e);
        }
    }

    private void thenResponseIsEmpty(Optional<GeminiTextResponse> response) {
        assertTrue(response.isEmpty());
    }

    private void thenResponseHasExpectedData(Optional<GeminiTextResponse> response, 
                                             String expectedText, String expectedModel) {
        assertTrue(response.isPresent());
        assertEquals(expectedText, response.get().text());
        assertEquals(expectedModel, response.get().model());
    }

    private void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(bytes);
        }
    }
}
