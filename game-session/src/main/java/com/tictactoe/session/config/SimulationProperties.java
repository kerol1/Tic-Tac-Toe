package com.tictactoe.session.config;

import com.tictactoe.session.strategy.StrategyKind;
import com.tictactoe.session.strategy.StrategySettings;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/** Defaults for a session; a create request may override the strategy and the blunder rate. */
@Validated
@ConfigurationProperties(prefix = "simulation")
public record SimulationProperties(
        @DefaultValue("600ms") Duration tickDelay,
        @DefaultValue("heuristic") StrategyKind strategy,
        @DefaultValue("0.25") @DecimalMin("0") @DecimalMax("1") double blunderRate,
        @DefaultValue("3") @Positive int maxRecoveryCycles) {

    public SimulationProperties {
        if (tickDelay.isNegative()) {
            throw new IllegalArgumentException("simulation.tick-delay must be zero or positive");
        }
    }

    public StrategySettings defaultSettings() {
        return new StrategySettings(strategy, blunderRate);
    }
}
