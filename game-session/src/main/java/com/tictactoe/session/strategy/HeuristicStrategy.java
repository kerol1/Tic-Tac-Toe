package com.tictactoe.session.strategy;

import com.tictactoe.session.domain.Board;
import com.tictactoe.session.domain.Player;

import java.util.List;
import java.util.Optional;
import java.util.random.RandomGenerator;

/**
 * Win if possible, otherwise block the opponent's win, otherwise prefer the center,
 * then a corner, then any free cell. Ties among equally good cells are broken randomly
 * so consecutive games do not look identical.
 */
public final class HeuristicStrategy implements MoveStrategy {

    private static final List<List<Integer>> LINES = List.of(
            List.of(0, 1, 2), List.of(3, 4, 5), List.of(6, 7, 8),
            List.of(0, 3, 6), List.of(1, 4, 7), List.of(2, 5, 8),
            List.of(0, 4, 8), List.of(2, 4, 6));
    private static final int CENTER = 4;
    private static final List<Integer> CORNERS = List.of(0, 2, 6, 8);

    private final RandomGenerator random;

    public HeuristicStrategy(RandomGenerator random) {
        this.random = random;
    }

    @Override
    public int next(Board board, Player player) {
        return completingMove(board, player)
                .or(() -> completingMove(board, player.opponent()))
                .or(() -> board.cell(CENTER) == null ? Optional.of(CENTER) : Optional.empty())
                .or(() -> pick(CORNERS.stream().filter(corner -> board.cell(corner) == null).toList()))
                .or(() -> pick(board.freePositions()))
                .orElseThrow(() -> new IllegalStateException("No free cell to play"));
    }

    /** The free cell that would complete a line for {@code player}, if any. */
    private static Optional<Integer> completingMove(Board board, Player player) {
        return LINES.stream()
                .map(line -> missingCell(board, line, player))
                .flatMap(Optional::stream)
                .findFirst();
    }

    private static Optional<Integer> missingCell(Board board, List<Integer> line, Player player) {
        long owned = line.stream().filter(cell -> board.cell(cell) == player).count();
        List<Integer> free = line.stream().filter(cell -> board.cell(cell) == null).toList();
        return owned == 2 && free.size() == 1 ? Optional.of(free.getFirst()) : Optional.empty();
    }

    private Optional<Integer> pick(List<Integer> candidates) {
        return candidates.isEmpty() ? Optional.empty() : Optional.of(candidates.get(random.nextInt(candidates.size())));
    }
}
