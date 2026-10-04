package com.aiplatform.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CodeReviewRequest(
        @NotBlank(message = "language is required")
        @Size(max = 50, message = "language must be at most 50 characters")
        String language,

        @NotBlank(message = "code is required")
        @Size(max = 20000, message = "code must be at most 20000 characters")
        String code
) {
}
