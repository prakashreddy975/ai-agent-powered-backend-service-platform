package com.aiplatform.ai.service;

import com.aiplatform.ai.client.AiReviewClient;
import com.aiplatform.ai.dto.CodeReviewRequest;
import com.aiplatform.ai.dto.CodeReviewResponse;
import com.aiplatform.common.exception.AiNotConfiguredException;
import com.aiplatform.common.exception.AiServiceException;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class CodeReviewService {

    private final AiReviewClient aiReviewClient;

    public CodeReviewService(AiReviewClient aiReviewClient) {
        this.aiReviewClient = aiReviewClient;
    }

    public CodeReviewResponse review(CodeReviewRequest request) {
        if (!aiReviewClient.isConfigured()) {
            throw new AiNotConfiguredException(
                    "AI code review is not configured. Set the ANTHROPIC_API_KEY environment variable and restart the application.");
        }
        String language = request.language().trim().toLowerCase(Locale.ROOT);
        CodeReviewResponse response = aiReviewClient.reviewCode(language, request.code());
        if (response == null || response.summary() == null || response.summary().isBlank()) {
            throw new AiServiceException("AI provider returned an empty review");
        }
        return response;
    }
}
