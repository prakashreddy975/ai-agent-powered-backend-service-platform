package com.aiplatform.ai.dto;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.List;

/**
 * Structured code review result. The same record is used as the JSON schema
 * Claude must answer with, so the API response is exactly what Claude produced.
 */
public record CodeReviewResponse(
        @JsonPropertyDescription("Two or three sentence overall assessment of the code")
        String summary,

        @JsonPropertyDescription("Problems found in the code; empty if none")
        List<ReviewIssue> issues,

        @JsonPropertyDescription("General improvement recommendations not tied to a single issue")
        List<String> recommendations
) {

    public CodeReviewResponse {
        issues = issues == null ? List.of() : List.copyOf(issues);
        recommendations = recommendations == null ? List.of() : List.copyOf(recommendations);
    }
}
