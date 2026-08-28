package com.tictactoe.session.engine;

/**
 * The Engine could not be reached, or answered in a way that leaves the outcome unknown
 * (timeout, connection reset, 5xx).
 */
public class EngineUnavailableException extends RuntimeException {

    public EngineUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }

    public EngineUnavailableException(String message) {
        super(message);
    }
}
