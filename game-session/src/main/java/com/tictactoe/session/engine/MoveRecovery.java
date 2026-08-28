package com.tictactoe.session.engine;

import com.tictactoe.session.config.SimulationProperties;
import com.tictactoe.session.domain.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * The simulation's view of the Engine. Submits a move so that an ambiguous failure
 * (timeout, reset, 5xx) never double-applies it and never loses it: after such a
 * failure the game is re-read, and because a cell is written at most once,
 * {@code board[position] == player} tells exactly whether the move landed.
 */
@Component
public class MoveRecovery {

    private static final Logger log = LoggerFactory.getLogger(MoveRecovery.class);

    private final EngineClient engine;
    private final int maxCycles;

    public MoveRecovery(EngineClient engine, SimulationProperties properties) {
        this.engine = engine;
        this.maxCycles = properties.maxRecoveryCycles();
    }

    public GameState current(UUID gameId) {
        return engine.getGame(gameId);
    }

    public GameState submit(UUID gameId, Player player, int position) {
        for (int cycle = 1; cycle <= maxCycles; cycle++) {
            try {
                return engine.submitMove(gameId, player, position);
            } catch (EngineUnavailableException ambiguous) {
                GameState state = engine.getGame(gameId);
                if (state.hasSymbolAt(position, player)) {
                    log.info("Move landed despite the failed response gameId={} position={} cycle={}",
                            gameId, position, cycle);
                    return state;
                }
                log.warn("Move did not land, retrying gameId={} position={} cycle={}/{}",
                        gameId, position, cycle, maxCycles);
            }
        }
        throw new EngineUnavailableException(
                "Gave up submitting move " + position + " on " + gameId + " after " + maxCycles + " cycles");
    }
}
