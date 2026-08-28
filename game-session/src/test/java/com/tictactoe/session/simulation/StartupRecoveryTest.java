package com.tictactoe.session.simulation;

import com.tictactoe.session.domain.SessionState;
import com.tictactoe.session.persistence.SessionEntity;
import com.tictactoe.session.persistence.SessionRepository;
import com.tictactoe.session.support.SessionTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class StartupRecoveryTest extends SessionTestBase {

    @Autowired
    private SessionRepository sessions;

    @Autowired
    private StartupRecovery recovery;

    @Test
    void sessionsLeftRunningByADeadProcessAreClosedAsFailed() {
        UUID stuck = createSession();
        UUID untouched = createSession();
        assertThat(sessions.startSimulation(stuck, Instant.now())).isEqualTo(1);

        int swept = recovery.sweep();

        assertThat(swept).isGreaterThanOrEqualTo(1);
        assertThat(sessions.findById(stuck)).get().satisfies(session -> {
            assertThat(session.getState()).isEqualTo(SessionState.FAILED);
            assertThat(session.getFailureReason()).isEqualTo(GameLoop.INTERNAL_ERROR);
        });
        assertThat(sessions.findById(untouched)).get()
                .extracting(SessionEntity::getState).isEqualTo(SessionState.CREATED);
    }

    @Test
    void theTransitionQueryIsACompareAndSet() {
        UUID id = createSession();

        assertThat(sessions.startSimulation(id, Instant.now())).isEqualTo(1);
        assertThat(sessions.startSimulation(id, Instant.now())).isEqualTo(0);
    }
}
