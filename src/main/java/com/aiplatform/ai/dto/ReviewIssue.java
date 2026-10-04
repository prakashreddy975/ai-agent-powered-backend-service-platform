package com.aiplatform.ai.dto;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

public record ReviewIssue(
        @JsonPropertyDescription("How serious the issue is")
        IssueSeverity severity,

        @JsonPropertyDescription("Where the issue is, e.g. 'line 12' or 'method calculateTotal'; 'general' if not tied to one place")
        String location,

        @JsonPropertyDescription("One-sentence description of the problem")
        String description,

        @JsonPropertyDescription("Concrete suggestion for fixing it")
        String suggestion
) {
}
