package com.tictactoe.engine.api;

import com.tictactoe.engine.service.GameService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/games")
@Tag(name = "Games", description = "Board state, move validation and outcome")
public class GameController {

    private final GameService gameService;

    public GameController(GameService gameService) {
        this.gameService = gameService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a game with a client-supplied id")
    public GameStateResponse create(@Valid @RequestBody CreateGameRequest request) {
        return GameStateResponse.from(gameService.createGame(request.gameId()));
    }

    @GetMapping("/{gameId}")
    @Operation(summary = "Current board and status")
    public GameStateResponse get(@PathVariable UUID gameId) {
        return GameStateResponse.from(gameService.getGame(gameId));
    }

    @PostMapping("/{gameId}/moves")
    @Operation(summary = "Submit a move; returns the updated state or a 409 with the rejection reason")
    public GameStateResponse move(@PathVariable UUID gameId, @Valid @RequestBody MoveRequest request) {
        return GameStateResponse.from(gameService.makeMove(gameId, request.toMove()));
    }
}
