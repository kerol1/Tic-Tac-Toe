package com.tictactoe.session.engine;

import com.tictactoe.session.domain.Board;
import com.tictactoe.session.domain.GameStatus;
import com.tictactoe.session.domain.Player;

import java.util.List;
import java.util.UUID;

/**
 * The Engine's view of a game, as returned by every Engine endpoint. {@code nextPlayer}
 * is null once the game has ended.
 */
public record GameState(UUID gameId, List<Player> board, Player nextPlayer, GameStatus status, List<Integer> winningLine) {

    public Board toBoard() {
        return new Board(board);
    }

    public boolean isFinished() {
        return status.isTerminal();
    }

    public boolean hasSymbolAt(int position, Player player) {
        return board.get(position) == player;
    }
}
