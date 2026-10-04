package com.aiplatform.integration.core;

import com.aiplatform.common.exception.ExternalApiException;
import org.springframework.core.codec.CodecException;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.TimeoutException;

/**
 * Shared plumbing for WebClient-based integrations: a configured client,
 * an overall timeout, and translation of every failure mode (timeout,
 * upstream error status, connection failure, malformed body) into an
 * {@link ExternalApiException} the REST layer knows how to render.
 */
public abstract class AbstractExternalApiClient implements ExternalIntegration {

    private final String baseUrl;
    private final Duration timeout;
    protected final WebClient webClient;

    protected AbstractExternalApiClient(WebClient.Builder builder, String baseUrl, Duration timeout) {
        this.baseUrl = baseUrl;
        this.timeout = timeout;
        this.webClient = builder.clone().baseUrl(baseUrl).build();
    }

    @Override
    public String baseUrl() {
        return baseUrl;
    }

    /**
     * Blocks on the call with a timeout and maps any failure to {@link ExternalApiException}.
     * A 404 from upstream becomes a 404 with {@code notFoundMessage}; everything else is a 502/504.
     */
    protected <T> T execute(Mono<T> call, String notFoundMessage) {
        try {
            T result = call.timeout(timeout).block();
            if (result == null) {
                throw malformed("empty response body", null);
            }
            return result;
        } catch (WebClientResponseException e) {
            if (e.getStatusCode().value() == HttpStatus.NOT_FOUND.value()) {
                throw new ExternalApiException(name(), HttpStatus.NOT_FOUND, notFoundMessage, e);
            }
            throw new ExternalApiException(name(), HttpStatus.BAD_GATEWAY,
                    name() + " API returned HTTP " + e.getStatusCode().value(), e);
        } catch (WebClientRequestException e) {
            if (hasCause(e, TimeoutException.class) || hasCause(e, io.netty.handler.timeout.ReadTimeoutException.class)) {
                throw timedOut(e);
            }
            throw new ExternalApiException(name(), HttpStatus.BAD_GATEWAY, "Could not reach the " + name() + " API", e);
        } catch (CodecException e) {
            throw malformed("response could not be parsed", e);
        } catch (ExternalApiException e) {
            throw e;
        } catch (RuntimeException e) {
            // Reactor wraps the checked TimeoutException raised by Mono#timeout
            if (hasCause(e, TimeoutException.class)) {
                throw timedOut(e);
            }
            throw new ExternalApiException(name(), HttpStatus.BAD_GATEWAY, name() + " API call failed", e);
        }
    }

    /** Use when a required field is missing from an otherwise valid response. */
    protected <V> V require(V value, String field) {
        if (value == null || (value instanceof String s && s.isBlank())) {
            throw malformed("missing field '" + field + "'", null);
        }
        return value;
    }

    private ExternalApiException timedOut(Throwable cause) {
        return new ExternalApiException(name(), HttpStatus.GATEWAY_TIMEOUT,
                name() + " API did not respond within " + timeout.toSeconds() + "s", cause);
    }

    private ExternalApiException malformed(String detail, Throwable cause) {
        return new ExternalApiException(name(), HttpStatus.BAD_GATEWAY,
                name() + " API returned a malformed response: " + detail, cause);
    }

    private static boolean hasCause(Throwable error, Class<? extends Throwable> type) {
        for (Throwable t = error; t != null; t = t.getCause()) {
            if (type.isInstance(t)) {
                return true;
            }
        }
        return false;
    }
}
