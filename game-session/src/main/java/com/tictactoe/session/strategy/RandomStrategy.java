package com.tictactoe.session.strategy;

import com.tictactoe.session.domain.Board;
import com.tictactoe.session.domain.Player;

import java.util.List;
import java.util.random.RandomGenerator;

public final class RandomStrategy implements MoveStrategy {

    private final RandomGenerator random;

    public RandomStrategy(RandomGenerator random) {
        this.random = random;
    }

    @Override
    public int next(Board board, Player player) {
        List<Integer> free = board.freePositions();
        if (free.isEmpty()) {
            throw new IllegalStateException("No free cell to play");
        }
        return free.get(random.nextInt(free.size()));
    }
}
