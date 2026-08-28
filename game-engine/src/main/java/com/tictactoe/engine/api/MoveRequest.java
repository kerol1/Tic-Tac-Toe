package com.tictactoe.engine.api;

import com.tictactoe.engine.domain.Move;
import com.tictactoe.engine.domain.Position;
import com.tictactoe.engine.domain.Symbol;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record MoveRequest(
        @NotNull Symbol player,
        @NotNull @Min(Position.MIN) @Max(Position.MAX) Integer position) {

    public Move toMove() {
        return new Move(player, new Position(position));
    }
}
