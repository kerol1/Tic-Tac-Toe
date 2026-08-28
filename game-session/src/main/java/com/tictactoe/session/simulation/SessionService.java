package com.tictactoe.session.simulation;

import com.tictactoe.session.engine.EngineClient;
import com.tictactoe.session.engine.GameState;
import com.tictactoe.session.persistence.SessionEntity;
import com.tictactoe.session.persistence.SessionRepository;
import com.tictactoe.session.strategy.StrategySettings;
import com.tictactoe.session.web.RequestIdFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

@Service
public class SessionService {

    private static final Logger log = LoggerFactory.getLogger(SessionService.class);

    private final SessionRepository sessions;
    private final EngineClient engine;
    private final GameLoop gameLoop;
    private final ExecutorService simulationExecutor;
    private final Clock clock;

    public SessionService(SessionRepository sessions, EngineClient engine, GameLoop gameLoop,
                          ExecutorService simulationExecutor, Clock clock) {
        this.sessions = sessions;
        this.engine = engine;
        this.gameLoop = gameLoop;
        this.simulationExecutor = simulationExecutor;
        this.clock = clock;
    }

    /**
     * Engine first, database second: if the Engine cannot create the game there is no
     * session to keep. Not transactional on purpose — the Engine call must not hold a
     * connection.
     */
    public SessionDetails create(StrategySettings settings) {
        UUID id = UUID.randomUUID();
        GameState game = engine.createGame(id);
        SessionEntity entity = sessions.save(SessionEntity.created(id, game, settings, clock.instant()));
        log.info("Session created sessionId={} strategy={} blunderRate={}", id, settings.strategy(), settings.blunderRate());
        return SessionDetails.of(entity, List.of());
    }

    /** The read fetches the settings the loop needs; the compare-and-set decides who starts. */
    public void startSimulation(UUID id) {
        SessionEntity entity = sessions.findById(id).orElseThrow(() -> new SessionNotFoundException(id));
        if (sessions.startSimulation(id, clock.instant()) == 0) {
            throw new SimulationAlreadyStartedException(id, entity.getState());
        }
        StrategySettings settings = entity.getSettings();
        String requestId = MDC.get(RequestIdFilter.MDC_KEY);
        simulationExecutor.submit(() -> gameLoop.run(id, settings, requestId));
        log.info("Simulation started sessionId={}", id);
    }
}
