package com.tictactoe.session.simulation;

import com.tictactoe.session.persistence.MoveRecordRepository;
import com.tictactoe.session.persistence.SessionEntity;
import com.tictactoe.session.persistence.SessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Read side of sessions. Answers from the persisted snapshot and history only — it never
 * talks to the Engine, so reads stay cheap and keep working while the Engine is down.
 */
@Service
public class SessionQueryService {

    private final SessionRepository sessions;
    private final MoveRecordRepository moves;

    public SessionQueryService(SessionRepository sessions, MoveRecordRepository moves) {
        this.sessions = sessions;
        this.moves = moves;
    }

    @Transactional(readOnly = true)
    public SessionDetails get(UUID id) {
        SessionEntity entity = sessions.findById(id).orElseThrow(() -> new SessionNotFoundException(id));
        return SessionDetails.of(entity, moves.findBySessionIdOrderByMoveNumber(id));
    }
}
