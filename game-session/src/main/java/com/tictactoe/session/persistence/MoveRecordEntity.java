package com.tictactoe.session.persistence;

import com.tictactoe.session.domain.GameStatus;
import com.tictactoe.session.domain.Player;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "move_records",
        uniqueConstraints = @UniqueConstraint(columnNames = {"session_id", "move_number"}))
public class MoveRecordEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false)
    private UUID sessionId;

    @Column(name = "move_number", nullable = false)
    private int moveNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 1)
    private Player player;

    @Column(nullable = false)
    private int position;

    @Enumerated(EnumType.STRING)
    @Column(name = "resulting_status", nullable = false, length = 16)
    private GameStatus resultingStatus;

    @Column(nullable = false)
    private Instant createdAt;

    protected MoveRecordEntity() {
    }

    public MoveRecordEntity(UUID sessionId, int moveNumber, Player player, int position,
                            GameStatus resultingStatus, Instant createdAt) {
        this.sessionId = sessionId;
        this.moveNumber = moveNumber;
        this.player = player;
        this.position = position;
        this.resultingStatus = resultingStatus;
        this.createdAt = createdAt;
    }

    public int getMoveNumber() {
        return moveNumber;
    }

    public Player getPlayer() {
        return player;
    }

    public int getPosition() {
        return position;
    }

    public GameStatus getResultingStatus() {
        return resultingStatus;
    }
}
