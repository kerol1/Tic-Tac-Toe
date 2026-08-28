package com.tictactoe.session.simulation;

import com.tictactoe.session.domain.SessionState;

import java.util.UUID;

public class SimulationAlreadyStartedException extends RuntimeException {

    public SimulationAlreadyStartedException(UUID id, SessionState state) {
        super("Session " + id + " is " + state + "; a simulation can only start from CREATED");
    }
}
