package com.aiplatform.integration.core;

import io.netty.channel.ChannelOption;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

/**
 * Builds the {@link WebClient.Builder} used by all integrations, with
 * connect and response timeouts applied at the HTTP client level.
 */
@Configuration
public class WebClientConfig {

    @Bean
    public WebClient.Builder integrationWebClientBuilder(IntegrationProperties properties) {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) properties.connectTimeout().toMillis())
                .responseTimeout(properties.readTimeout());
        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeader("User-Agent", "ai-agent-powered-backend-service-platform");
    }
}
