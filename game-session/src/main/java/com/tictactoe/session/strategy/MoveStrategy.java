package com.tictactoe.session.strategy;

import com.tictactoe.session.domain.Board;
import com.tictactoe.session.domain.Player;

/**
 * Picks the next position for a player on a board that still has free cells.
 */
public sealed interface MoveStrategy permits HeuristicStrategy, RandomStrategy {

    int next(Board board, Player player);
}
