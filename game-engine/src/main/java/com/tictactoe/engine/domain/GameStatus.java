package com.tictactoe.engine.domain;

public enum GameStatus {
    IN_PROGRESS, X_WON, O_WON, DRAW;

    public boolean isTerminal() {
        return this != IN_PROGRESS;
    }

    public static GameStatus wonBy(Symbol symbol) {
        return symbol == Symbol.X ? X_WON : O_WON;
    }
}
