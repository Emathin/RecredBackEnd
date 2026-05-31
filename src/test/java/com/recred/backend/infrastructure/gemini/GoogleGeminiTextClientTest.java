package com.recred.backend.infrastructure.gemini;

import com.google.genai.Client;
import com.recred.backend.config.GeminiProperties;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GoogleGeminiTextClientTest {

    private static final String MODEL = "gemini-test";
    private static final String VISION_MODEL = "gemini-3.1-flash-lite";
    private static final String API_KEY = "test-api-key";
    private static final String BASE_PATH = "/v1beta/models/" + MODEL + ":generateContent";
    private static final String VISION_PATH = "/v1beta/models/" + VISION_MODEL + ":generateContent";

    @Test
    void noGenerarTextoCuandoGeminiNoEstaConfigurado() {
        GoogleGeminiTextClient client = new GoogleGeminiTextClient(new GeminiProperties(
                "",
                MODEL,
                true
        ));

        Optional<GeminiTextResponse> response = client.generate("prompt");

        assertTrue(response.isEmpty());
    }

    @Test
    void generarTextoConRespuestaValidaDeGemini() throws IOException {
        HttpServer server = iniciarServidorGemini(BASE_PATH, 200, """
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
            configurarBaseUrl(server);
            GoogleGeminiTextClient client = clienteConfigurado();

            Optional<GeminiTextResponse> response = client.generate("prompt");

            assertTrue(response.isPresent());
            assertEquals("{\"resumen\":\"Todo bien\"}", response.orElseThrow().text());
            assertEquals(MODEL, response.orElseThrow().model());
        } finally {
            cerrarServidor(server);
        }
    }

    @Test
    void devolverVacioCuandoGeminiDevuelveTextoEnBlanco() throws IOException {
        HttpServer server = iniciarServidorGemini(BASE_PATH, 200, """
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
            configurarBaseUrl(server);
            GoogleGeminiTextClient client = clienteConfigurado();

            Optional<GeminiTextResponse> response = client.generate("prompt");

            assertTrue(response.isEmpty());
        } finally {
            cerrarServidor(server);
        }
    }

    @Test
    void devolverVacioCuandoGeminiFalla() throws IOException {
        HttpServer server = iniciarServidorGemini(BASE_PATH, 400, """
                {
                  "error": {
                    "code": 400,
                    "message": "Solicitud invalida",
                    "status": "INVALID_ARGUMENT"
                  }
                }
                """);

        try {
            configurarBaseUrl(server);
            GoogleGeminiTextClient client = clienteConfigurado();

            Optional<GeminiTextResponse> response = client.generate("prompt");

            assertTrue(response.isEmpty());
        } finally {
            cerrarServidor(server);
        }
    }

    @Test
    void noGenerarTextoConImagenCuandoGeminiNoEstaConfigurado() {
        GoogleGeminiTextClient client = new GoogleGeminiTextClient(new GeminiProperties(
                "",
                MODEL,
                true
        ));
        MockMultipartFile file = new MockMultipartFile("image", "test.jpg", "image/jpeg", "content".getBytes());

        Optional<GeminiTextResponse> response = client.generateWithImage("prompt", file);

        assertTrue(response.isEmpty());
    }

    @Test
    void generarTextoConImagenConRespuestaValida() throws IOException {
        HttpServer server = iniciarServidorGemini(VISION_PATH, 200, """
                {
                  "candidates": [
                    {
                      "content": {
                        "parts": [
                          {
                            "text": "{\\"name\\":\\"Coca Cola\\"}"
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
            configurarBaseUrl(server);
            GoogleGeminiTextClient client = clienteConfigurado();
            MockMultipartFile file = new MockMultipartFile("image", "test.jpg", "image/jpeg", "content".getBytes());

            Optional<GeminiTextResponse> response = client.generateWithImage("prompt", file);

            assertTrue(response.isPresent());
            assertEquals("{\"name\":\"Coca Cola\"}", response.orElseThrow().text());
            assertEquals(VISION_MODEL, response.orElseThrow().model());
        } finally {
            cerrarServidor(server);
        }
    }

    @Test
    void devolverVacioCuandoFallaLecturaDeImagen() throws IOException {
        GoogleGeminiTextClient client = clienteConfigurado();
        MultipartFile file = mock(MultipartFile.class);
        when(file.getBytes()).thenThrow(new IOException("Read error"));

        Optional<GeminiTextResponse> response = client.generateWithImage("prompt", file);

        assertTrue(response.isEmpty());
    }

    @Test
    void devolverVacioCuandoGeminiFallaConImagen() throws IOException {
        HttpServer server = iniciarServidorGemini(VISION_PATH, 500, "Error");

        try {
            configurarBaseUrl(server);
            GoogleGeminiTextClient client = clienteConfigurado();
            MockMultipartFile file = new MockMultipartFile("image", "test.jpg", "image/jpeg", "content".getBytes());

            Optional<GeminiTextResponse> response = client.generateWithImage("prompt", file);

            assertTrue(response.isEmpty());
        } finally {
            cerrarServidor(server);
        }
    }

    private GoogleGeminiTextClient clienteConfigurado() {
        return new GoogleGeminiTextClient(new GeminiProperties(
                API_KEY,
                MODEL,
                true
        ));
    }

    private HttpServer iniciarServidorGemini(String path, int status, String body) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext(path, exchange -> responder(exchange, status, body));
        server.start();
        return server;
    }

    private void configurarBaseUrl(HttpServer server) {
        Client.setDefaultBaseUrls(
                Optional.of("http://localhost:" + server.getAddress().getPort()),
                Optional.empty()
        );
    }

    private void cerrarServidor(HttpServer server) {
        Client.setDefaultBaseUrls(Optional.empty(), Optional.empty());
        server.stop(0);
    }

    private void responder(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(bytes);
        }
    }
}
