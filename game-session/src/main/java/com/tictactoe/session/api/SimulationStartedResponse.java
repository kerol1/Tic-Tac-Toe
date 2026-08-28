package com.tictactoe.session.api;

import com.tictactoe.session.domain.SessionState;

import java.util.UUID;

public record SimulationStartedResponse(UUID sessionId, SessionState state) {
}
