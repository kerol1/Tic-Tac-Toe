package com.tictactoe.engine.domain;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * One playthrough. Immutable: applying a move yields a new instance.
 */
public final class Game {

    private final UUID id;
    private final Board board;
    private final Symbol nextPlayer;
    private final GameStatus status;
    private final WinningLine winningLine;

    private Game(UUID id, Board board, Symbol nextPlayer, GameStatus status, WinningLine winningLine) {
        this.id = Objects.requireNonNull(id, "id");
        this.board = Objects.requireNonNull(board, "board");
        this.nextPlayer = nextPlayer;
        this.status = Objects.requireNonNull(status, "status");
        this.winningLine = winningLine;
    }

    public static Game create(UUID id) {
        return new Game(id, Board.empty(), Symbol.X, GameStatus.IN_PROGRESS, null);
    }

    public MoveResult apply(Move move) {
        if (status.isTerminal()) {
            return new MoveResult.Rejected(RejectionReason.GAME_FINISHED);
        }
        if (move.player() != nextPlayer) {
            return new MoveResult.Rejected(RejectionReason.WRONG_TURN);
        }
        if (!board.isFree(move.position())) {
            return new MoveResult.Rejected(RejectionReason.CELL_OCCUPIED);
        }
        Board updatedBoard = board.withMove(move.position(), move.player());
        WinningLine line = GameRules.findWinningLine(updatedBoard, move.player()).orElse(null);
        GameStatus updatedStatus = GameRules.statusAfter(updatedBoard, move.player(), line != null);
        Symbol next = updatedStatus.isTerminal() ? null : move.player().opponent();
        return new MoveResult.Applied(new Game(id, updatedBoard, next, updatedStatus, line));
    }

    public UUID id() {
        return id;
    }

    public Board board() {
        return board;
    }

    /** Empty once the game has ended. */
    public Optional<Symbol> nextPlayer() {
        return Optional.ofNullable(nextPlayer);
    }

    public GameStatus status() {
        return status;
    }

    public Optional<WinningLine> winningLine() {
        return Optional.ofNullable(winningLine);
    }
}
