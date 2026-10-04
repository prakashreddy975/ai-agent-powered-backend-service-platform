package com.aiplatform.integration.github;

import java.time.Instant;

public record GitHubRepositoryInfo(
        String fullName,
        String description,
        String url,
        String language,
        String license,
        long stars,
        long forks,
        long openIssues,
        String defaultBranch,
        Instant lastPushedAt
) {
}
