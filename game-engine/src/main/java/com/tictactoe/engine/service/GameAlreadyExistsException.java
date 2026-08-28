package com.tictactoe.engine.service;

import java.util.UUID;

public class GameAlreadyExistsException extends RuntimeException {

    public GameAlreadyExistsException(UUID id) {
        super("Game " + id + " already exists");
    }
}
