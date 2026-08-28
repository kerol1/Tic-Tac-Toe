package com.tictactoe.session.persistence;

import com.tictactoe.session.domain.Board;
import com.tictactoe.session.domain.GameStatus;
import com.tictactoe.session.domain.Player;
import com.tictactoe.session.domain.SessionState;
import com.tictactoe.session.engine.GameState;
import com.tictactoe.session.strategy.StrategyKind;
import com.tictactoe.session.strategy.StrategySettings;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "sessions")
public class SessionEntity {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private SessionState state;

    @Column(length = 64)
    private String failureReason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private StrategyKind strategy;

    @Column(nullable = false)
    private double blunderRate;

    @Convert(converter = BoardConverter.class)
    @Column(nullable = false, length = 9)
    private Board board;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private GameStatus gameStatus;

    @Enumerated(EnumType.STRING)
    @Column(length = 1)
    private Player nextPlayer;

    @Convert(converter = WinningLineConverter.class)
    @Column(length = 5)
    private List<Integer> winningLine;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected SessionEntity() {
    }

    /** A new session mirrors the game exactly as the Engine created it. */
    public static SessionEntity created(UUID id, GameState game, StrategySettings settings, Instant now) {
        SessionEntity entity = new SessionEntity();
        entity.id = id;
        entity.state = SessionState.CREATED;
        entity.strategy = settings.strategy();
        entity.blunderRate = settings.blunderRate();
        entity.createdAt = now;
        entity.snapshot(game, now);
        return entity;
    }

    public void snapshot(GameState game, Instant now) {
        this.board = game.toBoard();
        this.gameStatus = game.status();
        this.nextPlayer = game.nextPlayer();
        this.winningLine = game.winningLine();
        this.updatedAt = now;
    }

    public void finish(Instant now) {
        this.state = SessionState.FINISHED;
        this.updatedAt = now;
    }

    public void fail(String reason, Instant now) {
        this.state = SessionState.FAILED;
        this.failureReason = reason;
        this.updatedAt = now;
    }

    public UUID getId() {
        return id;
    }

    public SessionState getState() {
        return state;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public StrategySettings getSettings() {
        return new StrategySettings(strategy, blunderRate);
    }

    public Board getBoard() {
        return board;
    }

    public GameStatus getGameStatus() {
        return gameStatus;
    }

    public Player getNextPlayer() {
        return nextPlayer;
    }

    public List<Integer> getWinningLine() {
        return winningLine;
    }
}
