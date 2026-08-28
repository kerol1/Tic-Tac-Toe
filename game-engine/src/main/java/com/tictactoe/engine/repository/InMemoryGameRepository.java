package com.tictactoe.engine.repository;

import com.tictactoe.engine.domain.Game;
import com.tictactoe.engine.domain.Move;
import com.tictactoe.engine.domain.MoveResult;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

@Repository
public class InMemoryGameRepository implements GameRepository {

    private final Map<UUID, Game> games = new ConcurrentHashMap<>();

    @Override
    public boolean saveIfAbsent(Game game) {
        return games.putIfAbsent(game.id(), game) == null;
    }

    @Override
    public Optional<Game> findById(UUID id) {
        return Optional.ofNullable(games.get(id));
    }

    /**
     * Validate-and-apply runs inside {@code compute}, which the map executes atomically
     * for a given key: two moves racing on one game are serialized here, and the loser
     * is judged against the board the winner produced.
     */
    @Override
    public Optional<MoveResult> applyMove(UUID id, Move move) {
        AtomicReference<MoveResult> outcome = new AtomicReference<>();
        games.computeIfPresent(id, (key, current) -> {
            MoveResult result = current.apply(move);
            outcome.set(result);
            return result instanceof MoveResult.Applied(Game game) ? game : current;
        });
        return Optional.ofNullable(outcome.get());
    }
}
