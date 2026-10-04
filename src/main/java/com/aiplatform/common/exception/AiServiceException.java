package com.aiplatform.common.exception;

/**
 * Thrown when the Claude API call fails or returns a response we cannot use.
 */
public class AiServiceException extends RuntimeException {

    public AiServiceException(String message) {
        super(message);
    }

    public AiServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
