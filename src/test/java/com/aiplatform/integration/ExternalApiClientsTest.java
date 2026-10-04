package com.aiplatform.integration;

import com.aiplatform.common.exception.ExternalApiException;
import com.aiplatform.integration.core.IntegrationProperties;
import com.aiplatform.integration.depsdev.DepsDevApiClient;
import com.aiplatform.integration.depsdev.LatestPackageVersion;
import com.aiplatform.integration.github.GitHubApiClient;
import com.aiplatform.integration.github.GitHubRepositoryInfo;
import com.aiplatform.integration.osv.OsvApiClient;
import com.aiplatform.integration.osv.VulnerabilityReport;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration clients against a local MockWebServer: success, upstream errors,
 * malformed responses and timeouts. No real third-party calls are made.
 */
class ExternalApiClientsTest {

    private MockWebServer server;
    private GitHubApiClient gitHub;
    private DepsDevApiClient depsDev;
    private OsvApiClient osv;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        String url = server.url("/").toString().replaceAll("/$", "");
        IntegrationProperties properties = new IntegrationProperties(
                Duration.ofSeconds(1), Duration.ofMillis(500), url, url, url);
        gitHub = new GitHubApiClient(WebClient.builder(), properties);
        depsDev = new DepsDevApiClient(WebClient.builder(), properties);
        osv = new OsvApiClient(WebClient.builder(), properties);
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    private void enqueueJson(int status, String body) {
        server.enqueue(new MockResponse().setResponseCode(status)
                .setHeader("Content-Type", "application/json").setBody(body));
    }

    @Test
    void gitHubMapsRepository() throws InterruptedException {
        enqueueJson(200, """
                {"full_name": "spring-projects/spring-boot", "html_url": "https://github.com/spring-projects/spring-boot",
                 "description": "Spring Boot", "language": "Java", "license": {"spdx_id": "Apache-2.0"},
                 "stargazers_count": 10, "forks_count": 5, "open_issues_count": 2,
                 "default_branch": "main", "pushed_at": "2026-10-02T17:22:42Z", "extra": true}
                """);

        GitHubRepositoryInfo info = gitHub.getRepository("spring-projects", "spring-boot");

        assertThat(info.fullName()).isEqualTo("spring-projects/spring-boot");
        assertThat(info.license()).isEqualTo("Apache-2.0");
        assertThat(info.stars()).isEqualTo(10);
        RecordedRequest request = server.takeRequest(1, TimeUnit.SECONDS);
        assertThat(request.getPath()).isEqualTo("/repos/spring-projects/spring-boot");
    }

    @Test
    void upstream404BecomesNotFound() {
        enqueueJson(404, "{\"message\": \"Not Found\"}");

        assertThatThrownBy(() -> gitHub.getRepository("nobody", "nothing"))
                .isInstanceOfSatisfying(ExternalApiException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void upstream500BecomesBadGateway() {
        enqueueJson(500, "{}");

        assertThatThrownBy(() -> gitHub.getRepository("a", "b"))
                .isInstanceOfSatisfying(ExternalApiException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_GATEWAY);
                    assertThat(e.getMessage()).contains("HTTP 500");
                });
    }

    @Test
    void invalidJsonIsReportedAsMalformed() {
        enqueueJson(200, "{not json");

        assertThatThrownBy(() -> gitHub.getRepository("a", "b"))
                .isInstanceOfSatisfying(ExternalApiException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_GATEWAY);
                    assertThat(e.getMessage()).contains("malformed");
                });
    }

    @Test
    void missingRequiredFieldIsReportedAsMalformed() {
        enqueueJson(200, "{\"description\": \"no name\"}");

        assertThatThrownBy(() -> gitHub.getRepository("a", "b"))
                .isInstanceOfSatisfying(ExternalApiException.class,
                        e -> assertThat(e.getMessage()).contains("full_name"));
    }

    @Test
    void slowResponseTimesOut() {
        server.enqueue(new MockResponse().setBody("{}").setHeader("Content-Type", "application/json")
                .setHeadersDelay(2, TimeUnit.SECONDS));

        assertThatThrownBy(() -> gitHub.getRepository("a", "b"))
                .isInstanceOfSatisfying(ExternalApiException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.GATEWAY_TIMEOUT));
    }

    @Test
    void depsDevReturnsDefaultVersionAndEncodesName() throws InterruptedException {
        enqueueJson(200, """
                {"versions": [
                  {"versionKey": {"version": "32.0.0-jre"}, "publishedAt": "2025-01-01T00:00:00Z", "isDefault": false},
                  {"versionKey": {"version": "33.0.0-jre"}, "publishedAt": "2026-01-01T00:00:00Z", "isDefault": true}
                ]}
                """);

        LatestPackageVersion latest = depsDev.getLatestVersion("Maven", "com.google.guava:guava");

        assertThat(latest.latestVersion()).isEqualTo("33.0.0-jre");
        assertThat(latest.knownVersions()).isEqualTo(2);
        assertThat(server.takeRequest(1, TimeUnit.SECONDS).getPath())
                .isEqualTo("/v3/systems/maven/packages/com.google.guava%3Aguava");
    }

    @Test
    void depsDevRejectsUnsupportedEcosystemWithoutCallingUpstream() {
        assertThatThrownBy(() -> depsDev.getLatestVersion("cobol", "x"))
                .isInstanceOfSatisfying(ExternalApiException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
        assertThat(server.getRequestCount()).isZero();
    }

    @Test
    void osvMapsVulnerabilities() throws InterruptedException {
        enqueueJson(200, """
                {"vulns": [{"id": "GHSA-1", "summary": "Bad thing", "aliases": ["CVE-2020-1"],
                            "published": "2020-05-15T18:58:44Z", "database_specific": {"severity": "HIGH"}}]}
                """);

        VulnerabilityReport report = osv.findVulnerabilities("Maven", "g:a", "1.0");

        assertThat(report.vulnerabilityCount()).isEqualTo(1);
        assertThat(report.vulnerabilities().get(0).severity()).isEqualTo("HIGH");
        assertThat(server.takeRequest(1, TimeUnit.SECONDS).getBody().readUtf8()).contains("\"ecosystem\":\"Maven\"");
    }

    @Test
    void osvEmptyObjectMeansNoVulnerabilities() {
        enqueueJson(200, "{}");

        VulnerabilityReport report = osv.findVulnerabilities("Maven", "g:a", "9.9");

        assertThat(report.vulnerabilityCount()).isZero();
        assertThat(report.vulnerabilities()).isEmpty();
    }
}
