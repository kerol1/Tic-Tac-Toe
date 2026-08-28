package com.tictactoe.engine.repository;

import com.tictactoe.engine.domain.Game;
import com.tictactoe.engine.domain.Move;
import com.tictactoe.engine.domain.MoveResult;

import java.util.Optional;
import java.util.UUID;

/**
 * Storage for games. Implementations must make {@link #applyMove} atomic per game so
 * that concurrent moves on the same game cannot interleave.
 */
public interface GameRepository {

    /** @return {@code false} when a game with the same id already exists */
    boolean saveIfAbsent(Game game);

    Optional<Game> findById(UUID id);

    /** @return empty when the game does not exist */
    Optional<MoveResult> applyMove(UUID id, Move move);
}
