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

    @Test
    void shouldNotGenerateTextWhenGeminiIsNotConfigured() {
        GoogleGeminiTextClient client = new GoogleGeminiTextClient(new GeminiProperties(
                "",
                MODEL,
                true
        ));

        Optional<GeminiTextResponse> response = client.generate("prompt");

        assertTrue(response.isEmpty());
    }

    @Test
    void shouldGenerateTextWithValidGeminiResponse() throws IOException {
        HttpServer server = startGeminiServer(BASE_PATH, 200, """
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
                """);

        try {
            configureBaseUrl(server);
            GoogleGeminiTextClient client = configuredClient();

            Optional<GeminiTextResponse> response = client.generate("prompt");

            assertTrue(response.isPresent());
            assertEquals("{\"resumen\":\"Todo bien\"}", response.orElseThrow().text());
            assertEquals(MODEL, response.orElseThrow().model());
        } finally {
            stopServer(server);
        }
    }

    @Test
    void shouldReturnEmptyWhenGeminiReturnsBlankText() throws IOException {
        HttpServer server = startGeminiServer(BASE_PATH, 200, """
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
                """);

        try {
            configureBaseUrl(server);
            GoogleGeminiTextClient client = configuredClient();

            Optional<GeminiTextResponse> response = client.generate("prompt");

            assertTrue(response.isEmpty());
        } finally {
            stopServer(server);
        }
    }

    @Test
    void shouldReturnEmptyWhenGeminiFails() throws IOException {
        HttpServer server = startGeminiServer(BASE_PATH, 400, """
                {
                  "error": {
                    "code": 400,
                    "message": "Solicitud invalida",
                    "status": "INVALID_ARGUMENT"
                  }
                }
                """);

        try {
            configureBaseUrl(server);
            GoogleGeminiTextClient client = configuredClient();

            Optional<GeminiTextResponse> response = client.generate("prompt");

            assertTrue(response.isEmpty());
        } finally {
            stopServer(server);
        }
    }

    @Test
    void shouldNotGenerateTextWithImageWhenGeminiIsNotConfigured() throws IOException {
        GoogleGeminiTextClient client = new GoogleGeminiTextClient(new GeminiProperties(
                "",
                MODEL,
                true
        ));
        MockMultipartFile file = new MockMultipartFile(
                "image", "test.jpg", "image/jpeg", "content".getBytes());

        Optional<GeminiTextResponse> response = client.generateWithFile(
                "prompt", file.getBytes(), file.getContentType());

        assertTrue(response.isEmpty());
    }

    @Test
    void shouldGenerateTextWithImageWithValidResponse() throws IOException {
        HttpServer server = startGeminiServer(VISION_PATH, 200, """
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
                """);

        try {
            configureBaseUrl(server);
            GoogleGeminiTextClient client = configuredClient();
            MockMultipartFile file = new MockMultipartFile(
                    "image", "test.jpg", "image/jpeg", "content".getBytes());

            Optional<GeminiTextResponse> response = client.generateWithFile(
                    "prompt", file.getBytes(), file.getContentType());

            assertTrue(response.isPresent());
            assertEquals("{\"nombre\":\"Coca Cola\"}", response.orElseThrow().text());
            assertEquals(VISION_MODEL, response.orElseThrow().model());
        } finally {
            stopServer(server);
        }
    }

    @Test
    void shouldReturnEmptyWhenGeminiFailsWithImage() throws IOException {
        HttpServer server = startGeminiServer(VISION_PATH, 400, "Error");

        try {
            configureBaseUrl(server);
            GoogleGeminiTextClient client = configuredClient();
            MockMultipartFile file = new MockMultipartFile(
                    "image", "test.jpg", "image/jpeg", "content".getBytes());

            Optional<GeminiTextResponse> response = client.generateWithFile(
                    "prompt", file.getBytes(), file.getContentType());

            assertTrue(response.isEmpty());
        } finally {
            stopServer(server);
        }
    }

    private GoogleGeminiTextClient configuredClient() {
        return new GoogleGeminiTextClient(new GeminiProperties(
                API_KEY,
                MODEL,
                true
        ));
    }

    private HttpServer startGeminiServer(String path, int status, String body) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext(path, exchange -> respond(exchange, status, body));
        server.start();
        return server;
    }

    private void configureBaseUrl(HttpServer server) {
        Client.setDefaultBaseUrls(
                Optional.of("http://localhost:" + server.getAddress().getPort()),
                Optional.empty()
        );
    }

    private void stopServer(HttpServer server) {
        Client.setDefaultBaseUrls(Optional.empty(), Optional.empty());
        server.stop(0);
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
