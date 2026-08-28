package com.tictactoe.e2e;

import org.testcontainers.containers.ComposeContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.containers.wait.strategy.WaitStrategy;

import java.io.File;
import java.time.Duration;

/**
 * The real docker-compose topology, built once from the repository's own compose file
 * and shared by every end-to-end test class. The engine publishes no host port by
 * design; Testcontainers reaches it through an ambassador container instead.
 */
final class Stack {

    static final String FRONTEND = "frontend";
    static final String SESSION = "game-session";
    static final String ENGINE = "game-engine";
    static final String DISCOVERY = "discovery";

    private static final Duration STARTUP = Duration.ofMinutes(3);

    static final ComposeContainer COMPOSE = new ComposeContainer(new File("../docker-compose.yml"))
            .withLocalCompose(true)
            .withBuild(true)
            .withEnv("SIMULATION_TICK_DELAY", "200ms")
            .withExposedService(ENGINE, 8081, healthy(8081))
            .withExposedService(SESSION, 8082, healthy(8082))
            .withExposedService(DISCOVERY, 8761, healthy(8761))
            // A 404 from the session service for a made-up id proves the whole browser
            // path resolves: nginx -> gateway -> registry lookup -> session. Before the
            // registry has the instance the gateway answers 503 instead.
            .withExposedService(FRONTEND, 80, Wait.forHttp("/api/sessions/00000000-0000-0000-0000-000000000000")
                    .forPort(80).forStatusCode(404).withStartupTimeout(STARTUP));

    static {
        COMPOSE.start();
        Runtime.getRuntime().addShutdownHook(new Thread(COMPOSE::stop));
    }

    private Stack() {
    }

    static String url(String service, int port) {
        return "http://" + COMPOSE.getServiceHost(service, port) + ":" + COMPOSE.getServicePort(service, port);
    }

    /** Everything a browser can reach, through nginx. */
    static String publicApi() {
        return url(FRONTEND, 80) + "/api";
    }

    private static WaitStrategy healthy(int port) {
        return Wait.forHttp("/actuator/health").forPort(port).forStatusCode(200).withStartupTimeout(STARTUP);
    }
}
