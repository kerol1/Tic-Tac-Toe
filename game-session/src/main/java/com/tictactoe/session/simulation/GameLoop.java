package com.tictactoe.session.simulation;

import com.tictactoe.session.config.SimulationProperties;
import com.tictactoe.session.domain.Player;
import com.tictactoe.session.engine.EngineRejectedException;
import com.tictactoe.session.engine.EngineUnavailableException;
import com.tictactoe.session.engine.GameState;
import com.tictactoe.session.engine.MoveRecovery;
import com.tictactoe.session.strategy.MoveStrategy;
import com.tictactoe.session.strategy.MoveStrategyFactory;
import com.tictactoe.session.strategy.StrategySettings;
import com.tictactoe.session.web.RequestIdFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

/**
 * Plays one session to its end: generate a move, submit it, record it, announce it,
 * pause, repeat. Any failure ends the session as FAILED; nothing is left RUNNING.
 */
@Component
public class GameLoop {

    public static final String ENGINE_UNAVAILABLE = "ENGINE_UNAVAILABLE";
    public static final String INTERNAL_ERROR = "INTERNAL_ERROR";

    private static final Logger log = LoggerFactory.getLogger(GameLoop.class);

    private final MoveRecovery engine;
    private final MoveStrategyFactory strategies;
    private final SessionWriter writer;
    private final ProgressEventPublisher publisher;
    private final Duration tick;

    public GameLoop(MoveRecovery engine, MoveStrategyFactory strategies, SessionWriter writer,
                    ProgressEventPublisher publisher, SimulationProperties properties) {
        this.engine = engine;
        this.strategies = strategies;
        this.writer = writer;
        this.publisher = publisher;
        this.tick = properties.tickDelay();
    }

    /** Runs on its own thread; {@code requestId} keeps the log trail of the request that started it. */
    public void run(UUID sessionId, StrategySettings settings, String requestId) {
        MDC.put(RequestIdFilter.MDC_KEY, requestId);
        try {
            GameState finalState = playToTheEnd(sessionId, strategies.create(settings));
            announce(sessionId, new ProgressEvent.Finished(finalState.status(), finalState.winningLine(), finalState.board()));
            log.info("Simulation finished sessionId={} status={}", sessionId, finalState.status());
        } catch (EngineRejectedException rejected) {
            fail(sessionId, rejected.code(), rejected.getMessage(), rejected);
        } catch (EngineUnavailableException unavailable) {
            fail(sessionId, ENGINE_UNAVAILABLE, "The game engine is unavailable", unavailable);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            fail(sessionId, INTERNAL_ERROR, "The simulation was interrupted", interrupted);
        } catch (RuntimeException unexpected) {
            fail(sessionId, INTERNAL_ERROR, "The simulation hit an unexpected error", unexpected);
        } finally {
            MDC.remove(RequestIdFilter.MDC_KEY);
        }
    }

    private GameState playToTheEnd(UUID sessionId, MoveStrategy strategy) throws InterruptedException {
        GameState state = engine.current(sessionId);
        int moveNumber = 0;
        while (!state.isFinished()) {
            Player player = state.nextPlayer();
            if (player == null) {
                throw new IllegalStateException("In-progress game without a next player");
            }
            int position = strategy.next(state.toBoard(), player);
            state = engine.submit(sessionId, player, position);
            AppliedMove move = new AppliedMove(++moveNumber, player, position, state);
            writer.recordMove(sessionId, move);
            announce(sessionId, move.toEvent());
            log.info("Move recorded sessionId={} moveNumber={} player={} position={} gameStatus={}",
                    sessionId, moveNumber, player, position, state.status());
            if (!state.isFinished()) {
                pause();
            }
        }
        return state;
    }

    private void fail(UUID sessionId, String code, String message, Exception cause) {
        log.error("Simulation failed sessionId={} code={}", sessionId, code, cause);
        try {
            writer.fail(sessionId, code);
        } finally {
            announce(sessionId, new ProgressEvent.Failed(code, message));
        }
    }

    private void announce(UUID sessionId, ProgressEvent event) {
        try {
            publisher.publish(sessionId, event);
        } catch (RuntimeException spectatorProblem) {
            log.warn("Progress event dropped sessionId={} event={}", sessionId, event.eventName(), spectatorProblem);
        }
    }

    private void pause() throws InterruptedException {
        if (!tick.isZero()) {
            Thread.sleep(tick);
        }
    }
}
