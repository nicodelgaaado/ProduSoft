package com.produsoft.workflow.config;

import java.util.Optional;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@ConfigurationProperties(prefix = "app.ai.ollama")
public class OllamaClientProperties {

    /**
     * Base URL for Ollama's cloud API.
     */
    private String host = "https://ollama.com";

    /**
     * Default model that is used when the request does not specify one.
     */
    private String defaultModel = "gpt-oss:20b-cloud";

    /**
     * API key, usually supplied via {@code app.ai.ollama.api-key} property or the
     * {@code OLLAMA_API_KEY} environment variable.
     */
    private String apiKey;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getDefaultModel() {
        return defaultModel;
    }

    public void setDefaultModel(String defaultModel) {
        this.defaultModel = defaultModel;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public Optional<String> resolveApiKey() {
        if (StringUtils.hasText(apiKey)) {
            return Optional.of(apiKey.trim());
        }
        String envValue = System.getenv("OLLAMA_API_KEY");
        if (StringUtils.hasText(envValue)) {
            return Optional.of(envValue.trim());
        }
        return Optional.empty();
    }
}
