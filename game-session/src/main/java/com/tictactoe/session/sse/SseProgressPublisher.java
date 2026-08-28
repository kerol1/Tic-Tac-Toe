package com.tictactoe.session.sse;

import com.tictactoe.session.domain.Board;
import com.tictactoe.session.domain.SessionState;
import com.tictactoe.session.simulation.ProgressEvent;
import com.tictactoe.session.simulation.ProgressEventPublisher;
import com.tictactoe.session.simulation.SessionDetails;
import com.tictactoe.session.simulation.SessionQueryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Keeps one emitter list per session and fans progress events out to it.
 *
 * <p>Subscribing registers the emitter <em>before</em> reading the history it replays,
 * so a move published in between is delivered twice rather than never; clients
 * de-duplicate on {@code moveNumber}.
 */
@Component
public class SseProgressPublisher implements ProgressEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(SseProgressPublisher.class);
    private static final Duration EMITTER_TIMEOUT = Duration.ofMinutes(10);

    private final Map<UUID, List<SseEmitter>> emitters = new ConcurrentHashMap<>();
    private final SessionQueryService sessions;

    public SseProgressPublisher(SessionQueryService sessions) {
        this.sessions = sessions;
    }

    public SseEmitter subscribe(UUID sessionId) {
        sessions.get(sessionId);
        SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT.toMillis());
        register(sessionId, emitter);
        // Flushes the headers right away so clients see the stream as open before any event.
        trySend(sessionId, emitter, SseEmitter.event().comment("connected"));
        replay(sessions.get(sessionId), emitter);
        return emitter;
    }

    @Override
    public void publish(UUID sessionId, ProgressEvent event) {
        List<SseEmitter> subscribers = emitters.getOrDefault(sessionId, List.of());
        subscribers.forEach(emitter -> send(sessionId, emitter, event));
        if (isTerminal(event)) {
            subscribers.forEach(SseEmitter::complete);
        }
    }

    @Scheduled(fixedRateString = "PT15S")
    public void heartbeat() {
        emitters.forEach((sessionId, subscribers) -> subscribers.forEach(
                emitter -> trySend(sessionId, emitter, SseEmitter.event().comment("keep-alive"))));
    }

    private void register(UUID sessionId, SseEmitter emitter) {
        emitters.computeIfAbsent(sessionId, ignored -> new CopyOnWriteArrayList<>()).add(emitter);
        emitter.onCompletion(() -> drop(sessionId, emitter));
        emitter.onTimeout(() -> drop(sessionId, emitter));
        emitter.onError(ignored -> drop(sessionId, emitter));
    }

    /** Walks the history once, rebuilding each intermediate board as it goes. */
    private void replay(SessionDetails details, SseEmitter emitter) {
        Board board = Board.empty();
        for (SessionDetails.MoveRecord move : details.moves()) {
            board = board.with(move.position(), move.player());
            send(details.sessionId(), emitter,
                    new ProgressEvent.Move(move.moveNumber(), move.player(), move.position(), board.cells(), move.gameStatus()));
        }
        if (details.state() == SessionState.FINISHED) {
            send(details.sessionId(), emitter, new ProgressEvent.Finished(
                    details.game().status(), details.game().winningLine(), details.game().board()));
            emitter.complete();
        } else if (details.state() == SessionState.FAILED) {
            send(details.sessionId(), emitter, new ProgressEvent.Failed(details.failureReason(), "The simulation failed"));
            emitter.complete();
        }
    }

    private void send(UUID sessionId, SseEmitter emitter, ProgressEvent event) {
        SseEmitter.SseEventBuilder builder = SseEmitter.event().name(event.eventName()).data(event);
        if (event instanceof ProgressEvent.Move move) {
            builder.id(String.valueOf(move.moveNumber()));
        }
        trySend(sessionId, emitter, builder);
    }

    /** A subscriber that cannot be written to is simply forgotten; it is never the game's problem. */
    private void trySend(UUID sessionId, SseEmitter emitter, SseEmitter.SseEventBuilder builder) {
        try {
            emitter.send(builder);
        } catch (IOException | RuntimeException gone) {
            log.debug("Dropping SSE subscriber sessionId={} reason={}", sessionId, gone.getMessage());
            drop(sessionId, emitter);
        }
    }

    private void drop(UUID sessionId, SseEmitter emitter) {
        emitters.computeIfPresent(sessionId, (ignored, subscribers) -> {
            subscribers.remove(emitter);
            return subscribers.isEmpty() ? null : subscribers;
        });
    }

    private static boolean isTerminal(ProgressEvent event) {
        return event instanceof ProgressEvent.Finished || event instanceof ProgressEvent.Failed;
    }
}
