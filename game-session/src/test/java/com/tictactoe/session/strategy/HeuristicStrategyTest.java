package com.tictactoe.session.strategy;

import com.tictactoe.session.domain.Board;
import com.tictactoe.session.domain.Player;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

class HeuristicStrategyTest {

    private final HeuristicStrategy strategy = new HeuristicStrategy(new Random(1));

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
}
