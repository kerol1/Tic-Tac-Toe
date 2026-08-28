package com.tictactoe.session.api;

import com.tictactoe.session.config.SimulationProperties;
import com.tictactoe.session.strategy.StrategyKind;
import com.tictactoe.session.strategy.StrategySettings;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;

/** Both fields are optional; whatever is missing falls back to the configured default. */
public record CreateSessionRequest(
        StrategyKind strategy,
        @DecimalMin("0") @DecimalMax("1") Double blunderRate) {

    public StrategySettings settingsOr(SimulationProperties defaults) {
        return new StrategySettings(
                strategy == null ? defaults.strategy() : strategy,
                blunderRate == null ? defaults.blunderRate() : blunderRate);
    }
}
