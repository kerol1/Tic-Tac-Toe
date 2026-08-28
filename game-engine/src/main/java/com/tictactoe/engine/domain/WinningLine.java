package com.tictactoe.engine.domain;

import java.util.List;

/**
 * The three positions that produced a win.
 */
public record WinningLine(Position first, Position second, Position third) {

    public List<Integer> indexes() {
        return List.of(first.index(), second.index(), third.index());
    }
}
