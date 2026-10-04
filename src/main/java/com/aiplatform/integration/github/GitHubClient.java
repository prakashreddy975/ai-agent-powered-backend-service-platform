package com.aiplatform.integration.github;

public interface GitHubClient {

    GitHubRepositoryInfo getRepository(String owner, String repository);
}
