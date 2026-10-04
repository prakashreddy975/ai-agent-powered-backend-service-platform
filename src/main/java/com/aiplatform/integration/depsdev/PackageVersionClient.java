package com.aiplatform.integration.depsdev;

public interface PackageVersionClient {

    /**
     * @param ecosystem   package system, e.g. {@code maven}, {@code npm}, {@code pypi}
     * @param packageName package name as used by that ecosystem, e.g. {@code com.google.guava:guava}
     */
    LatestPackageVersion getLatestVersion(String ecosystem, String packageName);
}
