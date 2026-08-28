package com.tictactoe.session.simulation;

import com.tictactoe.session.domain.GameStatus;
import com.tictactoe.session.domain.Player;

import java.util.List;

/**
 * What subscribers learn about a running simulation. Each variant maps to one SSE event
 * type; {@link Finished} and {@link Failed} are terminal.
 */
public sealed interface ProgressEvent {

    String eventName();

    record Move(int moveNumber, Player player, int position, List<Player> board, GameStatus gameStatus)
            implements ProgressEvent {
        @Override
        public String eventName() {
            return "move";
        }
    }

    record Finished(GameStatus status, List<Integer> winningLine, List<Player> board) implements ProgressEvent {
        @Override
        public String eventName() {
            return "finished";
        }
    }

    /** Named {@code failed}, not {@code error}: browsers reserve {@code error} for transport problems. */
    record Failed(String code, String message) implements ProgressEvent {
        @Override
        public String eventName() {
            return "failed";
        }
    }
}
