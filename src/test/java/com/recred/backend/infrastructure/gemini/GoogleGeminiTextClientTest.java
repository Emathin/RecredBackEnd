package com.recred.backend.infrastructure.gemini;

import com.google.genai.Client;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.recred.backend.config.GeminiProperties;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GoogleGeminiTextClientTest {

    private static final String MODEL = "gemini-test";
    private static final String API_KEY = "test-api-key";
    private static final String BASE_PATH = "/v1beta/models/" + MODEL + ":generateContent";

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
        HttpServer server = iniciarServidorGemini(200, """
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
        HttpServer server = iniciarServidorGemini(200, """
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
        HttpServer server = iniciarServidorGemini(400, """
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

    private GoogleGeminiTextClient clienteConfigurado() {
        return new GoogleGeminiTextClient(new GeminiProperties(
                API_KEY,
                MODEL,
                true
        ));
    }

    private HttpServer iniciarServidorGemini(int status, String body) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext(BASE_PATH, exchange -> responder(exchange, status, body));
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
