package com.tictactoe.engine.domain;

import java.util.List;
import java.util.Optional;

/**
 * Pure rules of Tic Tac Toe: win and draw detection.
 */
public final class GameRules {

    private static final List<WinningLine> LINES = List.of(
            line(0, 1, 2), line(3, 4, 5), line(6, 7, 8),
            line(0, 3, 6), line(1, 4, 7), line(2, 5, 8),
            line(0, 4, 8), line(2, 4, 6));

    private GameRules() {
    }

    public static Optional<WinningLine> findWinningLine(Board board, Symbol symbol) {
        return LINES.stream()
                .filter(line -> board.cell(line.first()) == symbol
                        && board.cell(line.second()) == symbol
                        && board.cell(line.third()) == symbol)
                .findFirst();
    }

    /**
     * Status after {@code lastMover} has just placed a symbol. A completed line wins even
     * when it fills the board, so the win check runs before the draw check.
     */
    public static GameStatus evaluate(Board board, Symbol lastMover) {
        return statusAfter(board, lastMover, findWinningLine(board, lastMover).isPresent());
    }

    static GameStatus statusAfter(Board board, Symbol lastMover, boolean lineCompleted) {
        if (lineCompleted) {
            return GameStatus.wonBy(lastMover);
        }
        return board.isFull() ? GameStatus.DRAW : GameStatus.IN_PROGRESS;
    }

    private static WinningLine line(int first, int second, int third) {
        return new WinningLine(new Position(first), new Position(second), new Position(third));
    }
}
