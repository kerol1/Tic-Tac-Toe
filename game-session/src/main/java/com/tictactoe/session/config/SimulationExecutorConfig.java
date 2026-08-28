package com.tictactoe.session.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
@EnableScheduling
public class SimulationExecutorConfig {

    /** Each simulation runs on its own virtual thread; blocking on the Engine is cheap there. */
    @Bean(destroyMethod = "close")
    public ExecutorService simulationExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
