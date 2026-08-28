package com.tictactoe.engine.api;

import com.tictactoe.engine.domain.Game;
import com.tictactoe.engine.domain.GameStatus;
import com.tictactoe.engine.domain.Symbol;
import com.tictactoe.engine.domain.WinningLine;

import java.util.List;
import java.util.UUID;

public record GameStateResponse(
        UUID gameId,
        List<Symbol> board,
        Symbol nextPlayer,
        GameStatus status,
        List<Integer> winningLine) {

    public static GameStateResponse from(Game game) {
        return new GameStateResponse(
                game.id(),
                game.board().cells(),
                game.nextPlayer().orElse(null),
                game.status(),
                game.winningLine().map(WinningLine::indexes).orElse(null));
    }
}
