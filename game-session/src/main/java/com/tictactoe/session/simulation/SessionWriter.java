package com.tictactoe.session.simulation;

import com.tictactoe.session.persistence.MoveRecordEntity;
import com.tictactoe.session.persistence.MoveRecordRepository;
import com.tictactoe.session.persistence.SessionEntity;
import com.tictactoe.session.persistence.SessionRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

/**
 * Short write transactions used by the simulation loop, which itself runs outside any
 * transaction (it spends most of its time waiting on the Engine).
 */
@Component
public class SessionWriter {

    private final SessionRepository sessions;
    private final MoveRecordRepository moves;
    private final Clock clock;

    public SessionWriter(SessionRepository sessions, MoveRecordRepository moves, Clock clock) {
        this.sessions = sessions;
        this.moves = moves;
        this.clock = clock;
    }

    /**
     * Records the move and the resulting snapshot in one transaction. The move that ends
     * the game also finishes the session, so no crash window separates the two.
     */
    @Transactional
    public void recordMove(UUID sessionId, AppliedMove move) {
        SessionEntity session = load(sessionId);
        session.snapshot(move.result(), clock.instant());
        if (move.result().isFinished()) {
            session.finish(clock.instant());
        }
        moves.save(new MoveRecordEntity(sessionId, move.moveNumber(), move.player(), move.position(),
                move.result().status(), clock.instant()));
    }

    @Transactional
    public void fail(UUID sessionId, String reason) {
        load(sessionId).fail(reason, clock.instant());
    }

    private SessionEntity load(UUID sessionId) {
        return sessions.findById(sessionId).orElseThrow(() -> new SessionNotFoundException(sessionId));
    }
}
