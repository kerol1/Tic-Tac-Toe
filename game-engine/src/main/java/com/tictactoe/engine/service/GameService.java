package com.tictactoe.engine.service;

import com.tictactoe.engine.domain.Game;
import com.tictactoe.engine.domain.Move;
import com.tictactoe.engine.domain.MoveResult;
import com.tictactoe.engine.repository.GameRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class GameService {

    private static final Logger log = LoggerFactory.getLogger(GameService.class);

    private final GameRepository repository;

    public GameService(GameRepository repository) {
        this.repository = repository;
    }

    public Game createGame(UUID id) {
        Game game = Game.create(id);
        if (!repository.saveIfAbsent(game)) {
            throw new GameAlreadyExistsException(id);
        }
        log.info("Game created gameId={}", id);
        return game;
    }

    public Game getGame(UUID id) {
        return repository.findById(id).orElseThrow(() -> new GameNotFoundException(id));
    }

    public Game makeMove(UUID id, Move move) {
        MoveResult result = repository.applyMove(id, move)
                .orElseThrow(() -> new GameNotFoundException(id));
        return switch (result) {
            case MoveResult.Applied applied -> {
                log.info("Move applied gameId={} player={} position={} status={}",
                        id, move.player(), move.position().index(), applied.game().status());
                yield applied.game();
            }
            case MoveResult.Rejected rejected -> {
                log.info("Move rejected gameId={} player={} position={} reason={}",
                        id, move.player(), move.position().index(), rejected.reason());
                throw new MoveRejectedException(rejected.reason(), move);
            }
        };
    }
}
