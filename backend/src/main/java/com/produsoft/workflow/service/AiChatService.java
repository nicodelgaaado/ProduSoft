package com.produsoft.workflow.service;

import com.produsoft.workflow.config.OllamaClientProperties;
import com.produsoft.workflow.dto.AiChatRequest;
import com.produsoft.workflow.dto.AiChatResponse;
import com.produsoft.workflow.exception.AiClientException;
import java.util.Map;
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
    private volatile RestClient restClient;

    public AiChatService(RestClient.Builder restClientBuilder, OllamaClientProperties properties) {
        this.restClientBuilder = restClientBuilder;
        this.properties = properties;
    }

    public AiChatResponse chat(AiChatRequest request) {
        if (request.streamRequested()) {
            throw new AiClientException("Streaming responses are not supported yet.");
        }
        String model = StringUtils.hasText(request.model()) ? request.model() : properties.getDefaultModel();
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

    private record OllamaChatResponse(String model, OllamaMessage message, boolean done) {
    }

    private record OllamaMessage(String role, String content) {
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
            String apiKey = properties.resolveApiKey()
                .orElseThrow(() -> new AiClientException("""
                    Ollama access is not configured. Set the OLLAMA_API_KEY environment variable or app.ai.ollama.api-key property.""".
                    trim()));
            restClient = restClientBuilder
                .baseUrl(properties.getHost())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
            return restClient;
        }
    }
}
