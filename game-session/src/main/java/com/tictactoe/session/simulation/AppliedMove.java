package com.tictactoe.session.simulation;

import com.tictactoe.session.domain.Player;
import com.tictactoe.session.engine.GameState;

/** One move the Engine has accepted, with the game as it stood afterwards. */
public record AppliedMove(int moveNumber, Player player, int position, GameState result) {

    public ProgressEvent.Move toEvent() {
        return new ProgressEvent.Move(moveNumber, player, position, result.board(), result.status());
    }
}
