package com.aiplatform.project.dto;

import com.aiplatform.project.model.ProjectStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Payload for creating or updating a project. When {@code status} is omitted,
 * new projects default to {@link ProjectStatus#PLANNED}.
 */
public record ProjectRequest(
        @NotBlank(message = "name is required")
        @Size(max = 100, message = "name must be at most 100 characters")
        String name,

        @Size(max = 1000, message = "description must be at most 1000 characters")
        String description,

        ProjectStatus status
) {
}
