package com.aiplatform.common.exception;

/**
 * Thrown when the AI endpoint is called but no Claude API key has been configured.
 */
public class AiNotConfiguredException extends RuntimeException {

    public AiNotConfiguredException(String message) {
        super(message);
    }
}
