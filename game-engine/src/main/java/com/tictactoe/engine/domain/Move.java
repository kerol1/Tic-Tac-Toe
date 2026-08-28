package com.tictactoe.engine.domain;

import java.util.Objects;

public record Move(Symbol player, Position position) {

    public Move {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(position, "position");
    }
}
