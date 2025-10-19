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

    private final RestClient restClient;
    private final OllamaClientProperties properties;

    public AiChatService(RestClient.Builder restClientBuilder, OllamaClientProperties properties) {
        this.properties = properties;
        String apiKey = properties.resolveApiKey()
            .orElseThrow(() -> new AiClientException("OLLAMA_API_KEY is not configured."));
        this.restClient = restClientBuilder
            .baseUrl(properties.getHost())
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build();
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
            OllamaChatResponse response = restClient.post()
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
}
