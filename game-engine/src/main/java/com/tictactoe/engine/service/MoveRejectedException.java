package com.tictactoe.engine.service;

import com.tictactoe.engine.domain.Move;
import com.tictactoe.engine.domain.RejectionReason;

public class MoveRejectedException extends RuntimeException {

    private final RejectionReason reason;

    public MoveRejectedException(RejectionReason reason, Move move) {
        super(describe(reason, move));
        this.reason = reason;
    }

    public RejectionReason reason() {
        return reason;
    }

    private static String describe(RejectionReason reason, Move move) {
        return switch (reason) {
            case CELL_OCCUPIED -> "Cell " + move.position().index() + " is already occupied";
            case WRONG_TURN -> "It is not " + move.player() + "'s turn";
            case GAME_FINISHED -> "The game has already finished";
        };
    }
}
