package com.tictactoe.engine.domain;

/**
 * Index of a cell on the board: 0..8, left to right, top to bottom.
 */
public record Position(int index) {

    public static final int MIN = 0;
    public static final int MAX = 8;

    public Position {
        if (index < MIN || index > MAX) {
            throw new IllegalArgumentException("Position must be between 0 and 8, got " + index);
        }
    }
}
