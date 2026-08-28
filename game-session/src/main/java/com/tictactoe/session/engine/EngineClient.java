package com.tictactoe.session.engine;

import com.tictactoe.session.config.EngineProperties;
import com.tictactoe.session.domain.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Thin HTTP client for the Game Engine. Idempotent calls are retried on transport and
 * server errors; a move is never retried blindly (see {@link MoveRecovery}). Business
 * rejections (4xx) arrive as {@link EngineRejectedException} from the client's status handler.
 */
@Component
public class EngineClient {

    private static final Logger log = LoggerFactory.getLogger(EngineClient.class);
    private static final String GAME_ALREADY_EXISTS = "GAME_ALREADY_EXISTS";

    private final RestClient restClient;
    private final EngineProperties properties;

    public EngineClient(RestClient engineRestClient, EngineProperties properties) {
        this.restClient = engineRestClient;
        this.properties = properties;
    }

    /**
     * Creates the game. A {@code GAME_ALREADY_EXISTS} answer is treated as success: it
     * can only come from a retry whose first attempt landed but whose response was lost,
     * because ids are fresh UUIDs.
     */
    public GameState createGame(UUID gameId) {
        return withRetries("create game " + gameId, () -> {
            try {
                return restClient.post()
                        .uri("/games")
                        .body(Map.of("gameId", gameId))
                        .retrieve()
                        .body(GameState.class);
            } catch (EngineRejectedException rejected) {
                if (!GAME_ALREADY_EXISTS.equals(rejected.code())) {
                    throw rejected;
                }
                log.info("Game already existed on retry, treating as created gameId={}", gameId);
                return getGameOnce(gameId);
            }
        });
    }

    public GameState getGame(UUID gameId) {
        return withRetries("get game " + gameId, () -> getGameOnce(gameId));
    }

    /** Single attempt. Transport failures surface as {@link EngineUnavailableException}. */
    public GameState submitMove(UUID gameId, Player player, int position) {
        try {
            return restClient.post()
                    .uri("/games/{gameId}/moves", gameId)
                    .body(Map.of("player", player, "position", position))
                    .retrieve()
                    .body(GameState.class);
        } catch (HttpServerErrorException | ResourceAccessException transportError) {
            throw new EngineUnavailableException("Engine did not confirm move " + position + " on " + gameId, transportError);
        }
    }

    private GameState getGameOnce(UUID gameId) {
        return restClient.get()
                .uri("/games/{gameId}", gameId)
                .retrieve()
                .body(GameState.class);
    }

    private GameState withRetries(String action, Supplier<GameState> call) {
        int attempts = properties.maxRetries() + 1;
        EngineUnavailableException last = null;
        for (int attempt = 1; attempt <= attempts; attempt++) {
            try {
                return call.get();
            } catch (HttpServerErrorException | ResourceAccessException transportError) {
                last = new EngineUnavailableException("Engine unavailable: " + action, transportError);
                log.warn("Engine call failed action=\"{}\" attempt={}/{} cause={}",
                        action, attempt, attempts, transportError.getMessage());
                if (attempt < attempts) {
                    pause();
                }
            }
        }
        throw last;
    }

    private void pause() {
        try {
            Thread.sleep(properties.retryBackoff());
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new EngineUnavailableException("Interrupted while waiting to retry the Engine");
        }
    }
}
