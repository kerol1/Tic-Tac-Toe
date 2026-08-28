package com.tictactoe.engine.api;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateGameRequest(@NotNull UUID gameId) {
}
