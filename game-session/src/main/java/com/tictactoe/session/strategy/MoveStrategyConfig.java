package com.tictactoe.session.strategy;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.random.RandomGenerator;

@Configuration
public class MoveStrategyConfig {

    @Bean
    public RandomGenerator strategyRandom() {
        return RandomGenerator.getDefault();
    }
}
