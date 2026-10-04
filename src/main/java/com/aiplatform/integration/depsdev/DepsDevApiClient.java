package com.aiplatform.integration.depsdev;

import com.aiplatform.common.exception.ExternalApiException;
import com.aiplatform.integration.core.AbstractExternalApiClient;
import com.aiplatform.integration.core.IntegrationProperties;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Looks up the latest published version of an open-source package using
 * Google's Open Source Insights API (deps.dev). No credentials required.
 */
@Component
public class DepsDevApiClient extends AbstractExternalApiClient implements PackageVersionClient {

    static final Set<String> SUPPORTED_ECOSYSTEMS = Set.of("maven", "npm", "pypi", "go", "cargo", "nuget");

    public DepsDevApiClient(WebClient.Builder integrationWebClientBuilder, IntegrationProperties properties) {
        super(integrationWebClientBuilder, properties.depsDevBaseUrl(), properties.readTimeout());
    }

    @Override
    public String name() {
        return "deps-dev";
    }

    @Override
    public String description() {
        return "Latest published version of a Maven, npm, PyPI, Go, Cargo or NuGet package";
    }

    @Override
    public LatestPackageVersion getLatestVersion(String ecosystem, String packageName) {
        String system = ecosystem.trim().toLowerCase(Locale.ROOT);
        if (!SUPPORTED_ECOSYSTEMS.contains(system)) {
            throw new ExternalApiException(name(), HttpStatus.BAD_REQUEST,
                    "Unsupported ecosystem '" + ecosystem + "'. Supported: " + SUPPORTED_ECOSYSTEMS);
        }

        // The package name is a URI variable, so characters like ':' and '/' are percent-encoded
        PackagePayload payload = execute(
                webClient.get()
                        .uri("/v3/systems/{system}/packages/{name}", system, packageName)
                        .retrieve()
                        .bodyToMono(PackagePayload.class),
                "Package '" + packageName + "' was not found in " + system);

        List<VersionPayload> versions = require(payload.versions(), "versions");
        VersionPayload latest = versions.stream()
                .filter(VersionPayload::isDefault)
                .findFirst()
                .orElseThrow(() -> new ExternalApiException(name(), HttpStatus.BAD_GATEWAY,
                        name() + " API returned a malformed response: no default version"));

        return new LatestPackageVersion(
                system,
                packageName,
                require(latest.versionKey(), "versionKey").version(),
                latest.publishedAt(),
                latest.isDeprecated(),
                versions.size());
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record PackagePayload(List<VersionPayload> versions) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record VersionPayload(VersionKey versionKey, Instant publishedAt, boolean isDefault, boolean isDeprecated) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record VersionKey(String version) {
    }
}
