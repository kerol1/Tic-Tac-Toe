package com.tictactoe.session.simulation;

import com.tictactoe.session.persistence.SessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.Clock;

/**
 * A simulation only ever runs inside the process that started it. Whatever is still
 * RUNNING when the service starts belongs to a process that died mid-game and can never
 * resume, so it is closed as FAILED rather than left blocking re-inspection.
 */
@Component
public class StartupRecovery {

    private static final Logger log = LoggerFactory.getLogger(StartupRecovery.class);

    private final SessionRepository sessions;
    private final Clock clock;

    public StartupRecovery(SessionRepository sessions, Clock clock) {
        this.sessions = sessions;
        this.clock = clock;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        sweep();
    }

    public int sweep() {
        int failed = sessions.failAllRunning(GameLoop.INTERNAL_ERROR, clock.instant());
        if (failed > 0) {
            log.warn("Closed {} session(s) left RUNNING by a previous process", failed);
        }
        return failed;
    }
}
