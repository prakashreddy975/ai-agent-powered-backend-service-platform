package com.aiplatform.integration.core;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/**
 * Settings shared by the third-party integrations, bound from {@code integrations.*}.
 */
@ConfigurationProperties(prefix = "integrations")
public record IntegrationProperties(
        @DefaultValue("3s") Duration connectTimeout,
        @DefaultValue("10s") Duration readTimeout,
        @DefaultValue("https://api.github.com") String githubBaseUrl,
        @DefaultValue("https://api.deps.dev") String depsDevBaseUrl,
        @DefaultValue("https://api.osv.dev") String osvBaseUrl
) {
}
