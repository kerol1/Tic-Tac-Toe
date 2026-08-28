package com.tictactoe.session.config;

import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "simulation")
public record SimulationProperties(
        @DefaultValue("600ms") Duration tickDelay,
        @DefaultValue("heuristic") Strategy strategy,
        @DefaultValue("3") @Positive int maxRecoveryCycles) {

    public enum Strategy {
        HEURISTIC, RANDOM
    }

    public SimulationProperties {
        if (tickDelay.isNegative()) {
            throw new IllegalArgumentException("simulation.tick-delay must be zero or positive");
        }
    }
}
