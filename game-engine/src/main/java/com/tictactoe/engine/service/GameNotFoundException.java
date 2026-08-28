package com.tictactoe.engine.service;

import java.util.UUID;

public class GameNotFoundException extends RuntimeException {

    public GameNotFoundException(UUID id) {
        super("Game " + id + " not found");
    }
}
