package com.aiplatform.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown by the integration layer when a third-party API fails, times out or
 * returns a response that cannot be parsed. {@code status} is the status this
 * application should return to its own caller.
 */
public class ExternalApiException extends RuntimeException {

    private final String integration;
    private final HttpStatus status;

    public ExternalApiException(String integration, HttpStatus status, String message) {
        this(integration, status, message, null);
    }

    public ExternalApiException(String integration, HttpStatus status, String message, Throwable cause) {
        super(message, cause);
        this.integration = integration;
        this.status = status;
    }

    public String getIntegration() {
        return integration;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
