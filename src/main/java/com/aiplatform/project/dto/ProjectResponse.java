package com.aiplatform.project.dto;

import com.aiplatform.project.model.Project;
import com.aiplatform.project.model.ProjectStatus;

import java.time.Instant;

public record ProjectResponse(
        Long id,
        String name,
        String description,
        ProjectStatus status,
        Instant createdAt,
        Instant updatedAt
) {

    public static ProjectResponse from(Project project) {
        return new ProjectResponse(
                project.getId(),
                project.getName(),
                project.getDescription(),
                project.getStatus(),
                project.getCreatedAt(),
                project.getUpdatedAt());
    }
}
