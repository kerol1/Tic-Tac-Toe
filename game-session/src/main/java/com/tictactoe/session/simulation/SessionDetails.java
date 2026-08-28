package com.tictactoe.session.simulation;

import com.tictactoe.session.domain.GameStatus;
import com.tictactoe.session.domain.Player;
import com.tictactoe.session.domain.SessionState;
import com.tictactoe.session.persistence.MoveRecordEntity;
import com.tictactoe.session.persistence.SessionEntity;

import java.util.List;
import java.util.UUID;

/**
 * Read model of a session: its state, the last known game snapshot and the move history.
 */
public record SessionDetails(
        UUID sessionId,
        SessionState state,
        GameSnapshot game,
        List<MoveRecord> moves,
        String failureReason) {

    public record GameSnapshot(List<Player> board, Player nextPlayer, GameStatus status, List<Integer> winningLine) {
    }

    public record MoveRecord(int moveNumber, Player player, int position, GameStatus gameStatus) {
    }

    public static SessionDetails of(SessionEntity entity, List<MoveRecordEntity> history) {
        GameSnapshot snapshot = new GameSnapshot(
                entity.getBoard().cells(), entity.getNextPlayer(), entity.getGameStatus(), entity.getWinningLine());
        List<MoveRecord> moves = history.stream()
                .map(move -> new MoveRecord(move.getMoveNumber(), move.getPlayer(), move.getPosition(), move.getResultingStatus()))
                .toList();
        return new SessionDetails(entity.getId(), entity.getState(), snapshot, moves, entity.getFailureReason());
    }
}
