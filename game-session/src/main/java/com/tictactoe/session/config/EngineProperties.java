package com.tictactoe.session.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "engine")
public record EngineProperties(
        @NotBlank String baseUrl,
        @DefaultValue("false") boolean discovery,
        @DefaultValue("2s") Duration connectTimeout,
        @DefaultValue("5s") Duration readTimeout,
        @DefaultValue("2") @PositiveOrZero int maxRetries,
        @DefaultValue("200ms") Duration retryBackoff) {
}
