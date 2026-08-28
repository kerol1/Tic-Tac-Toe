package com.tictactoe.session.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MoveRecordRepository extends JpaRepository<MoveRecordEntity, Long> {

    List<MoveRecordEntity> findBySessionIdOrderByMoveNumber(UUID sessionId);
}
