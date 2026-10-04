package com.aiplatform.integration.core;

/**
 * Common contract for every third-party integration. Implementations are
 * discovered automatically, so adding a new integration only requires a new
 * Spring bean implementing this interface (plus its own domain interface).
 */
public interface ExternalIntegration {

    /** Short, stable identifier, e.g. {@code github}. */
    String name();

    /** One-line, human-readable description of what the integration provides. */
    String description();

    /** Base URL of the upstream API. */
    String baseUrl();
}
