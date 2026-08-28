package com.tictactoe.session.api;

import com.tictactoe.session.config.SimulationProperties;
import com.tictactoe.session.domain.SessionState;
import com.tictactoe.session.simulation.SessionDetails;
import com.tictactoe.session.simulation.SessionQueryService;
import com.tictactoe.session.simulation.SessionService;
import com.tictactoe.session.sse.SseProgressPublisher;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

@RestController
@RequestMapping("/sessions")
@Tag(name = "Sessions", description = "Automated game sessions and their live progress")
public class SessionController {

    private final SessionService sessions;
    private final SessionQueryService queries;
    private final SseProgressPublisher progress;
    private final SimulationProperties defaults;

    public SessionController(SessionService sessions, SessionQueryService queries, SseProgressPublisher progress,
                             SimulationProperties defaults) {
        this.sessions = sessions;
        this.queries = queries;
        this.progress = progress;
        this.defaults = defaults;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a session and its game in the engine; strategy and blunder rate are optional")
    public SessionDetails create(@Valid @RequestBody(required = false) CreateSessionRequest request) {
        CreateSessionRequest options = request == null ? new CreateSessionRequest(null, null) : request;
        return sessions.create(options.settingsOr(defaults));
    }

    @PostMapping("/{sessionId}/simulate")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "Start the automated simulation; returns immediately, progress is streamed")
    public SimulationStartedResponse simulate(@PathVariable UUID sessionId) {
        sessions.startSimulation(sessionId);
        return new SimulationStartedResponse(sessionId, SessionState.RUNNING);
    }

    @GetMapping("/{sessionId}")
    @Operation(summary = "Session state, last known board and full move history")
    public SessionDetails get(@PathVariable UUID sessionId) {
        return queries.get(sessionId);
    }

    @GetMapping(value = "/{sessionId}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Server-sent events: move, finished, failed. History is replayed on subscribe")
    public SseEmitter events(@PathVariable UUID sessionId) {
        return progress.subscribe(sessionId);
    }
}
