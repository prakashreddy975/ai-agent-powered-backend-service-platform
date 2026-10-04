package com.aiplatform.ai;

import com.aiplatform.ai.client.ClaudeAiReviewClient;
import com.aiplatform.common.exception.AiNotConfiguredException;
import com.aiplatform.config.ClaudeProperties;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ClaudeAiReviewClientTest {

    @Test
    void withoutApiKeyClientIsNotConfiguredAndFailsClearly() {
        ClaudeAiReviewClient client = new ClaudeAiReviewClient(
                new ClaudeProperties("  ", "claude-opus-5-5", 16000, Duration.ofSeconds(30)));

        assertThat(client.isConfigured()).isFalse();
        assertThatThrownBy(() -> client.reviewCode("java", "class A {}"))
                .isInstanceOf(AiNotConfiguredException.class)
                .hasMessageContaining("ANTHROPIC_API_KEY");
    }

    @Test
    void withApiKeyClientIsConfigured() {
        // Constructing the SDK client makes no network call
        ClaudeAiReviewClient client = new ClaudeAiReviewClient(
                new ClaudeProperties("test-key", "claude-opus-5-5", 16000, Duration.ofSeconds(30)));

        assertThat(client.isConfigured()).isTrue();
    }
}
