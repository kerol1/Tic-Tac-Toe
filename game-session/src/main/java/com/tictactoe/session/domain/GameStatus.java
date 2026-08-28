package com.tictactoe.session.domain;

/**
 * Game outcome as reported by the Game Engine.
 */
public enum GameStatus {
    IN_PROGRESS, X_WON, O_WON, DRAW;

    public boolean isTerminal() {
        return this != IN_PROGRESS;
    }
}
