package com.tictactoe.session.strategy;

import com.tictactoe.session.domain.Board;
import com.tictactoe.session.domain.Player;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.Map;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

class HeuristicStrategyTest {

    private static final int[][] LINES = {
            {0, 1, 2}, {3, 4, 5}, {6, 7, 8}, {0, 3, 6}, {1, 4, 7}, {2, 5, 8}, {0, 4, 8}, {2, 4, 6}};

    private final HeuristicStrategy strategy = new HeuristicStrategy(new Random(1), 0);

    @Test
    void takesTheWinningCellWhenAvailable() {
        Board board = Board.decode("XX.OO....");

        assertThat(strategy.next(board, Player.X)).isEqualTo(2);
    }

    @Test
    void blocksTheOpponentsWinWhenItCannotWinItself() {
        Board board = Board.decode("OO.X.....");

        assertThat(strategy.next(board, Player.X)).isEqualTo(2);
    }

    @Test
    void prefersWinningOverBlocking() {
        Board board = Board.decode("OO.XX....");

        assertThat(strategy.next(board, Player.X)).isEqualTo(5);
    }

    @Test
    void opensInTheCenter() {
        assertThat(strategy.next(Board.empty(), Player.X)).isEqualTo(4);
    }

    @Test
    void fallsBackToACornerWhenTheCenterIsTaken() {
        Board board = Board.decode("....X....");

        assertThat(strategy.next(board, Player.O)).isIn(0, 2, 6, 8);
    }

    @Test
    void twoPerfectPlayersAlwaysDraw() {
        Map<Outcome, Integer> outcomes = playOut(new HeuristicStrategy(new Random(3), 0), 500);

        assertThat(outcomes).containsOnlyKeys(Outcome.DRAW);
    }

    @Test
    void blundersLetEitherSideWin() {
        Map<Outcome, Integer> outcomes = playOut(new HeuristicStrategy(new Random(3), 0.25), 500);

        assertThat(outcomes).containsKeys(Outcome.X_WON, Outcome.O_WON, Outcome.DRAW);
    }

    @Test
    void aBlunderStillPlaysAFreeCell() {
        HeuristicStrategy alwaysBlunders = new HeuristicStrategy(new Random(5), 1);
        Board board = Board.decode("XX.OO.XO.");

        for (int attempt = 0; attempt < 50; attempt++) {
            assertThat(alwaysBlunders.next(board, Player.X)).isIn(2, 5, 8);
        }
    }

    private enum Outcome { X_WON, O_WON, DRAW }

    private static Map<Outcome, Integer> playOut(MoveStrategy strategy, int games) {
        Map<Outcome, Integer> outcomes = new EnumMap<>(Outcome.class);
        for (int game = 0; game < games; game++) {
            outcomes.merge(play(strategy), 1, Integer::sum);
        }
        return outcomes;
    }

    private static Outcome play(MoveStrategy strategy) {
        Board board = Board.empty();
        Player player = Player.X;
        while (!board.freePositions().isEmpty()) {
            board = board.with(strategy.next(board, player), player);
            if (hasLine(board, player)) {
                return player == Player.X ? Outcome.X_WON : Outcome.O_WON;
            }
            player = player.opponent();
        }
        return Outcome.DRAW;
    }

    private static boolean hasLine(Board board, Player player) {
        for (int[] line : LINES) {
            if (board.cell(line[0]) == player && board.cell(line[1]) == player && board.cell(line[2]) == player) {
                return true;
            }
        }
        return false;
    }
}
