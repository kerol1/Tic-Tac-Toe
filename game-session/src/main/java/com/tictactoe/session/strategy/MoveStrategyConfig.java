package com.tictactoe.session.strategy;

import com.tictactoe.session.config.SimulationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.random.RandomGenerator;

@Configuration
public class MoveStrategyConfig {

    @Bean
    public MoveStrategy moveStrategy(SimulationProperties properties, RandomGenerator random) {
        return switch (properties.strategy()) {
            case HEURISTIC -> new HeuristicStrategy(random);
            case RANDOM -> new RandomStrategy(random);
        };
    }

    @Bean
    public RandomGenerator strategyRandom() {
        return RandomGenerator.getDefault();
    }
}
