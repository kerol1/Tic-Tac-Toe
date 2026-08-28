package com.tictactoe.session.simulation;

import java.util.UUID;

/**
 * Fan-out seam between the simulation loop and whoever is watching. Implementations
 * must never throw back into the loop: a spectator problem is not a game problem.
 */
public interface ProgressEventPublisher {

    void publish(UUID sessionId, ProgressEvent event);
}
