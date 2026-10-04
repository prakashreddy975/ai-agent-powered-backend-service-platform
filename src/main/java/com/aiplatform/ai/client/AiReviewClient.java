package com.aiplatform.ai.client;

import com.aiplatform.ai.dto.CodeReviewResponse;

/**
 * Abstraction over the AI provider used for code review. The service layer
 * depends only on this interface, which keeps it provider-agnostic and lets
 * tests substitute a mock.
 */
public interface AiReviewClient {

    /**
     * @return {@code true} when the client has the credentials it needs to make calls
     */
    boolean isConfigured();

    CodeReviewResponse reviewCode(String language, String code);
}
