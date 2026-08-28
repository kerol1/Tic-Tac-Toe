package com.tictactoe.session.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

public interface SessionRepository extends JpaRepository<SessionEntity, UUID> {

    /**
     * The only way a session moves from CREATED to RUNNING. The predicate makes the
     * transition a compare-and-set: of two concurrent callers exactly one sees 1 row.
     */
    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update SessionEntity s set s.state = com.tictactoe.session.domain.SessionState.RUNNING, "
            + "s.updatedAt = :now where s.id = :id and s.state = com.tictactoe.session.domain.SessionState.CREATED")
    int startSimulation(@Param("id") UUID id, @Param("now") Instant now);

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update SessionEntity s set s.state = com.tictactoe.session.domain.SessionState.FAILED, "
            + "s.failureReason = :reason, s.updatedAt = :now "
            + "where s.state = com.tictactoe.session.domain.SessionState.RUNNING")
    int failAllRunning(@Param("reason") String reason, @Param("now") Instant now);
}
