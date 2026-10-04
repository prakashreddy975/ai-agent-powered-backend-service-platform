package com.aiplatform.integration.controller;

import com.aiplatform.integration.core.ExternalIntegration;
import com.aiplatform.integration.depsdev.LatestPackageVersion;
import com.aiplatform.integration.depsdev.PackageVersionClient;
import com.aiplatform.integration.github.GitHubClient;
import com.aiplatform.integration.github.GitHubRepositoryInfo;
import com.aiplatform.integration.osv.VulnerabilityClient;
import com.aiplatform.integration.osv.VulnerabilityReport;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/api/integrations")
public class IntegrationController {

    private static final String GITHUB_NAME = "^[A-Za-z0-9_.-]{1,100}$";

    private final List<ExternalIntegration> integrations;
    private final GitHubClient gitHubClient;
    private final PackageVersionClient packageVersionClient;
    private final VulnerabilityClient vulnerabilityClient;

    public IntegrationController(List<ExternalIntegration> integrations,
                                 GitHubClient gitHubClient,
                                 PackageVersionClient packageVersionClient,
                                 VulnerabilityClient vulnerabilityClient) {
        this.integrations = integrations;
        this.gitHubClient = gitHubClient;
        this.packageVersionClient = packageVersionClient;
        this.vulnerabilityClient = vulnerabilityClient;
    }

    @GetMapping
    public List<IntegrationInfo> list() {
        return integrations.stream()
                .map(i -> new IntegrationInfo(i.name(), i.description(), i.baseUrl()))
                .sorted(Comparator.comparing(IntegrationInfo::name))
                .toList();
    }

    @GetMapping("/github/repos/{owner}/{repo}")
    public GitHubRepositoryInfo gitHubRepository(
            @PathVariable @Pattern(regexp = GITHUB_NAME, message = "invalid owner") String owner,
            @PathVariable @Pattern(regexp = GITHUB_NAME, message = "invalid repository name") String repo) {
        return gitHubClient.getRepository(owner, repo);
    }

    @GetMapping("/packages/latest")
    public LatestPackageVersion latestPackageVersion(
            @RequestParam @NotBlank String ecosystem,
            @RequestParam @NotBlank String name) {
        return packageVersionClient.getLatestVersion(ecosystem, name);
    }

    @GetMapping("/vulnerabilities")
    public VulnerabilityReport vulnerabilities(
            @RequestParam @NotBlank String ecosystem,
            @RequestParam @NotBlank String name,
            @RequestParam @NotBlank String version) {
        return vulnerabilityClient.findVulnerabilities(ecosystem, name, version);
    }
}
