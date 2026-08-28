package com.tictactoe.session.strategy;

import com.tictactoe.session.domain.Board;
import com.tictactoe.session.domain.Player;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

class RandomStrategyTest {

    @Test
    void onlyPlaysFreeCells() {
        RandomStrategy strategy = new RandomStrategy(new Random(7));
        Board board = Board.decode("XOXOXO.X.");

        for (int attempt = 0; attempt < 50; attempt++) {
            assertThat(strategy.next(board, Player.O)).isIn(6, 8);
        }
    }

    @Test
    void sameSeedProducesTheSameGame() {
        assertThat(playOut(new Random(42))).isEqualTo(playOut(new Random(42)));
    }

    private static List<Integer> playOut(Random random) {
        RandomStrategy strategy = new RandomStrategy(random);
        Board board = Board.empty();
        Player player = Player.X;
        List<Integer> moves = new ArrayList<>();
        while (!board.freePositions().isEmpty()) {
            int position = strategy.next(board, player);
            moves.add(position);
            board = board.with(position, player);
            player = player.opponent();
        }
        return moves;
    }
}
