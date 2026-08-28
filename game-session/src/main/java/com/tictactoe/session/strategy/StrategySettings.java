package com.tictactoe.session.strategy;

/**
 * How both players pick their moves in one session. {@code blunderRate} is the
 * probability that a heuristic player skips its reasoning for a move and plays at
 * random; it is ignored by the random strategy.
 */
public record StrategySettings(StrategyKind strategy, double blunderRate) {

    public StrategySettings {
        if (blunderRate < 0 || blunderRate > 1) {
            throw new IllegalArgumentException("blunderRate must be between 0 and 1, got " + blunderRate);
        }
    }
}
