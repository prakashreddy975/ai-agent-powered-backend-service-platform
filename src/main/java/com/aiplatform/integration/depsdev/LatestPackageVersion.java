package com.aiplatform.integration.depsdev;

import java.time.Instant;

public record LatestPackageVersion(
        String ecosystem,
        String packageName,
        String latestVersion,
        Instant publishedAt,
        boolean deprecated,
        int knownVersions
) {
}
