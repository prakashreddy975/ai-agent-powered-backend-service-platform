package com.aiplatform.integration.github;

import com.aiplatform.integration.core.AbstractExternalApiClient;
import com.aiplatform.integration.core.IntegrationProperties;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;

/**
 * Reads public repository metadata from the GitHub REST API (no token required,
 * subject to GitHub's unauthenticated rate limit).
 */
@Component
public class GitHubApiClient extends AbstractExternalApiClient implements GitHubClient {

    public GitHubApiClient(WebClient.Builder integrationWebClientBuilder, IntegrationProperties properties) {
        super(integrationWebClientBuilder, properties.githubBaseUrl(), properties.readTimeout());
    }

    @Override
    public String name() {
        return "github";
    }

    @Override
    public String description() {
        return "Public GitHub repository metadata (stars, forks, language, license)";
    }

    @Override
    public GitHubRepositoryInfo getRepository(String owner, String repository) {
        GitHubRepoPayload payload = execute(
                webClient.get()
                        .uri("/repos/{owner}/{repo}", owner, repository)
                        .accept(MediaType.parseMediaType("application/vnd.github+json"))
                        .retrieve()
                        .bodyToMono(GitHubRepoPayload.class),
                "GitHub repository '" + owner + "/" + repository + "' was not found");

        return new GitHubRepositoryInfo(
                require(payload.fullName(), "full_name"),
                payload.description(),
                require(payload.htmlUrl(), "html_url"),
                payload.language(),
                payload.license() != null ? payload.license().spdxId() : null,
                payload.stars(),
                payload.forks(),
                payload.openIssues(),
                payload.defaultBranch(),
                payload.pushedAt());
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record GitHubRepoPayload(
            @JsonProperty("full_name") String fullName,
            String description,
            @JsonProperty("html_url") String htmlUrl,
            String language,
            License license,
            @JsonProperty("stargazers_count") long stars,
            @JsonProperty("forks_count") long forks,
            @JsonProperty("open_issues_count") long openIssues,
            @JsonProperty("default_branch") String defaultBranch,
            @JsonProperty("pushed_at") Instant pushedAt
    ) {
        @JsonIgnoreProperties(ignoreUnknown = true)
        record License(@JsonProperty("spdx_id") String spdxId) {
        }
    }
}
