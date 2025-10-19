package com.produsoft.workflow.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.produsoft.workflow.config.OllamaClientProperties;
import com.produsoft.workflow.dto.AiChatRequest;
import com.produsoft.workflow.dto.AiChatResponse;
import com.produsoft.workflow.exception.AiClientException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.function.Consumer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Service
public class AiChatService {

    private final RestClient.Builder restClientBuilder;
    private final OllamaClientProperties properties;
    private final ObjectMapper objectMapper;
    private volatile RestClient restClient;
    private volatile HttpClient streamingClient;

    public AiChatService(RestClient.Builder restClientBuilder,
                         OllamaClientProperties properties,
                         ObjectMapper objectMapper) {
        this.restClientBuilder = restClientBuilder;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public AiChatResponse chat(AiChatRequest request) {
        if (request.streamRequested()) {
            throw new AiClientException("Streaming responses are not supported yet.");
        }
        String model = resolveModel(request.model());
        Map<String, Object> payload = Map.of(
            "model", model,
            "messages", request.messages().stream()
                .map(message -> Map.of(
                    "role", message.role(),
                    "content", message.content()))
                .toList(),
            "stream", Boolean.FALSE
        );
        try {
            OllamaChatResponse response = ensureClient().post()
                .uri("/api/chat")
                .body(payload)
                .retrieve()
                .body(OllamaChatResponse.class);
            if (response == null || response.message() == null || !StringUtils.hasText(response.message().content())) {
                throw new AiClientException("Received empty response from Ollama.");
            }
            return new AiChatResponse(
                response.model(),
                response.message().role(),
                response.message().content());
        } catch (RestClientResponseException ex) {
            throw new AiClientException("Ollama API error: " + ex.getResponseBodyAsString(), ex);
        } catch (RestClientException ex) {
            throw new AiClientException("Failed to call Ollama API", ex);
        }
    }

    public String chatStream(AiChatRequest request, Consumer<String> tokenConsumer) {
        if (!request.streamRequested()) {
            throw new AiClientException("Request does not enable streaming.");
        }
        String model = resolveModel(request.model());
        Map<String, Object> payload = Map.of(
            "model", model,
            "messages", request.messages().stream()
                .map(message -> Map.of(
                    "role", message.role(),
                    "content", message.content()))
                .toList(),
            "stream", Boolean.TRUE
        );
        byte[] body;
        try {
            body = objectMapper.writeValueAsBytes(payload);
        } catch (IOException ex) {
            throw new AiClientException("Failed to serialize Ollama request payload.", ex);
        }

        HttpRequest httpRequest = HttpRequest.newBuilder()
            .uri(URI.create(properties.getHost() + "/api/chat"))
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + resolveApiKeyOrThrow())
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .POST(HttpRequest.BodyPublishers.ofByteArray(body))
            .build();

        try {
            HttpResponse<InputStream> response = ensureStreamingClient()
                .send(httpRequest, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() >= 400) {
                String errorBody = readErrorBody(response.body());
                throw new AiClientException("Ollama API error: " + errorBody);
            }
            StringBuilder builder = new StringBuilder();
            try (InputStream inputStream = response.body();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.isBlank()) {
                        continue;
                    }
                    OllamaStreamChunk chunk = objectMapper.readValue(line, OllamaStreamChunk.class);
                    if (StringUtils.hasText(chunk.error())) {
                        throw new AiClientException("Ollama API error: " + chunk.error());
                    }
                    if (chunk.message() != null && chunk.message().content() != null) {
                        String delta = chunk.message().content();
                        builder.append(delta);
                        if (tokenConsumer != null) {
                            tokenConsumer.accept(delta);
                        }
                    }
                    if (Boolean.TRUE.equals(chunk.done())) {
                        break;
                    }
                }
            }
            return builder.toString();
        } catch (IOException ex) {
            throw new AiClientException("Failed to stream response from Ollama.", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new AiClientException("Interrupted while streaming response from Ollama.", ex);
        }
    }

    private String resolveModel(String requestedModel) {
        return StringUtils.hasText(requestedModel) ? requestedModel : properties.getDefaultModel();
    }

    private String readErrorBody(InputStream body) throws IOException {
        try (InputStream inputStream = body) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private record OllamaChatResponse(String model, OllamaMessage message, boolean done) {
    }

    private record OllamaMessage(String role, String content) {
    }

    private record OllamaStreamChunk(String model, OllamaMessage message, Boolean done, String error) {
    }

    private RestClient ensureClient() {
        RestClient existing = restClient;
        if (existing != null) {
            return existing;
        }
        synchronized (this) {
            if (restClient != null) {
                return restClient;
            }
            String apiKey = resolveApiKeyOrThrow();
            restClient = restClientBuilder
                .baseUrl(properties.getHost())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
            return restClient;
        }
    }

    private HttpClient ensureStreamingClient() {
        HttpClient existing = streamingClient;
        if (existing != null) {
            return existing;
        }
        synchronized (this) {
            if (streamingClient != null) {
                return streamingClient;
            }
            streamingClient = HttpClient.newBuilder().build();
            return streamingClient;
        }
    }

    private String resolveApiKeyOrThrow() {
        return properties.resolveApiKey()
            .orElseThrow(() -> new AiClientException("""
                Ollama access is not configured. Set the OLLAMA_API_KEY environment variable or app.ai.ollama.api-key property."""
                .trim()));
    }
}
