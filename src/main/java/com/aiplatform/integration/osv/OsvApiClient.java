package com.aiplatform.integration.osv;

import com.aiplatform.integration.core.AbstractExternalApiClient;
import com.aiplatform.integration.core.IntegrationProperties;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Checks a package version against the OSV.dev open-source vulnerability database.
 * No credentials required.
 */
@Component
public class OsvApiClient extends AbstractExternalApiClient implements VulnerabilityClient {

    public OsvApiClient(WebClient.Builder integrationWebClientBuilder, IntegrationProperties properties) {
        super(integrationWebClientBuilder, properties.osvBaseUrl(), properties.readTimeout());
    }

    @Override
    public String name() {
        return "osv";
    }

    @Override
    public String description() {
        return "Known security vulnerabilities for a specific package version (OSV.dev)";
    }

    @Override
    public VulnerabilityReport findVulnerabilities(String ecosystem, String packageName, String version) {
        Map<String, Object> query = Map.of(
                "version", version,
                "package", Map.of("name", packageName, "ecosystem", ecosystem));

        QueryPayload payload = execute(
                webClient.post()
                        .uri("/v1/query")
                        .bodyValue(query)
                        .retrieve()
                        .bodyToMono(QueryPayload.class),
                "Package '" + packageName + "' was not found in OSV");

        // OSV returns an empty object when the version has no known vulnerabilities
        List<VulnerabilityReport.Vulnerability> vulnerabilities = payload.vulns() == null
                ? List.of()
                : payload.vulns().stream().map(this::toVulnerability).toList();

        return new VulnerabilityReport(ecosystem, packageName, version, vulnerabilities.size(), vulnerabilities);
    }

    private VulnerabilityReport.Vulnerability toVulnerability(VulnPayload vuln) {
        return new VulnerabilityReport.Vulnerability(
                require(vuln.id(), "vulns[].id"),
                vuln.summary(),
                vuln.aliases() == null ? List.of() : vuln.aliases(),
                vuln.databaseSpecific() != null ? vuln.databaseSpecific().severity() : null,
                vuln.published());
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record QueryPayload(List<VulnPayload> vulns) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record VulnPayload(
            String id,
            String summary,
            List<String> aliases,
            Instant published,
            @JsonProperty("database_specific") DatabaseSpecific databaseSpecific
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record DatabaseSpecific(String severity) {
    }
}
