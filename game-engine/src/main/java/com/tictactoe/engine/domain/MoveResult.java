package com.tictactoe.engine.domain;

/**
 * Outcome of applying a move: either the updated game or the reason it was refused.
 */
public sealed interface MoveResult {

    record Applied(Game game) implements MoveResult {
    }

    record Rejected(RejectionReason reason) implements MoveResult {
    }
}
