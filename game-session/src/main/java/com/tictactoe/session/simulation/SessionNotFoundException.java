package com.tictactoe.session.simulation;

import java.util.UUID;

public class SessionNotFoundException extends RuntimeException {

    public SessionNotFoundException(UUID id) {
        super("Session " + id + " not found");
    }
}
