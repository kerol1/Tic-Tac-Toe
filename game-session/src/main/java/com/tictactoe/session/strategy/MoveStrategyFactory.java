package com.tictactoe.session.strategy;

import org.springframework.stereotype.Component;

import java.util.random.RandomGenerator;

@Component
public class MoveStrategyFactory {

    private final RandomGenerator random;

    public MoveStrategyFactory(RandomGenerator random) {
        this.random = random;
    }

    public MoveStrategy create(StrategySettings settings) {
        return switch (settings.strategy()) {
            case HEURISTIC -> new HeuristicStrategy(random, settings.blunderRate());
            case RANDOM -> new RandomStrategy(random);
        };
    }
}
