package com.tictactoe.session.api;

import com.tictactoe.session.engine.EngineRejectedException;
import com.tictactoe.session.engine.EngineUnavailableException;
import com.tictactoe.session.simulation.SessionNotFoundException;
import com.tictactoe.session.simulation.SimulationAlreadyStartedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(SessionNotFoundException.class)
    public ResponseEntity<ErrorResponse> sessionNotFound(SessionNotFoundException ex) {
        return respond(HttpStatus.NOT_FOUND, "SESSION_NOT_FOUND", ex.getMessage());
    }

    @ExceptionHandler(SimulationAlreadyStartedException.class)
    public ResponseEntity<ErrorResponse> alreadyStarted(SimulationAlreadyStartedException ex) {
        return respond(HttpStatus.CONFLICT, "SIMULATION_ALREADY_STARTED", ex.getMessage());
    }

    @ExceptionHandler(EngineUnavailableException.class)
    public ResponseEntity<ErrorResponse> engineUnavailable(EngineUnavailableException ex) {
        log.warn("Engine unavailable: {}", ex.getMessage());
        return respond(HttpStatus.SERVICE_UNAVAILABLE, "ENGINE_UNAVAILABLE", "The game engine is unavailable");
    }

    /** The engine's own vocabulary stays internal; callers see one session-owned code. */
    @ExceptionHandler(EngineRejectedException.class)
    public ResponseEntity<ErrorResponse> engineRejected(EngineRejectedException ex) {
        log.error("Engine rejected a request the session considered valid: {} {}", ex.code(), ex.getMessage());
        return respond(HttpStatus.BAD_GATEWAY, "ENGINE_REJECTED", "The game engine rejected the request");
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ErrorResponse> badRequest(Exception ex) {
        return respond(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Invalid request");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> noResource(NoResourceFoundException ex) {
        return respond(HttpStatus.NOT_FOUND, "NOT_FOUND", "No such resource");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> unexpected(Exception ex) {
        log.error("Unhandled exception", ex);
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Unexpected error");
    }

    /**
     * The content type is set explicitly so the error body is written even when the
     * request only accepts {@code text/event-stream} (an SSE subscription to a missing session).
     */
    private static ResponseEntity<ErrorResponse> respond(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON).body(ErrorResponse.of(code, message));
    }
}
