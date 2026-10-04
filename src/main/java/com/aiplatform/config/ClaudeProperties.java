package com.aiplatform.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/**
 * Claude API settings, bound from the {@code claude.*} properties. The API key
 * comes from the {@code ANTHROPIC_API_KEY} environment variable and may be empty.
 */
@ConfigurationProperties(prefix = "claude")
public record ClaudeProperties(
        String apiKey,
        @DefaultValue("claude-opus-5-5") String model,
        @DefaultValue("16000") long maxTokens,
        @DefaultValue("120s") Duration timeout
) {

    public boolean hasApiKey() {
        return apiKey != null && !apiKey.isBlank();
    }
}
