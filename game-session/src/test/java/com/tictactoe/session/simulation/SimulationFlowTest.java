package com.tictactoe.session.simulation;

import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.tictactoe.session.domain.GameStatus;
import com.tictactoe.session.domain.Player;
import com.tictactoe.session.domain.SessionState;
import com.tictactoe.session.persistence.MoveRecordEntity;
import com.tictactoe.session.persistence.MoveRecordRepository;
import com.tictactoe.session.support.FakeEngine.MoveFault;
import com.tictactoe.session.support.SessionTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;

import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.assertj.core.api.Assertions.assertThat;

class SimulationFlowTest extends SessionTestBase {

    @Autowired
    private MoveRecordRepository moveRecords;

    @Test
    void playsAFullGameAndRecordsEveryMoveInOrder() {
        UUID id = createSession();
        assertThat(simulate(id).getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);

        SessionDetails details = awaitState(id, SessionState.FINISHED);

        assertThat(details.game().status()).isIn(GameStatus.X_WON, GameStatus.O_WON, GameStatus.DRAW);
        assertThat(details.game().nextPlayer()).isNull();
        assertThat(details.moves()).isNotEmpty().hasSizeLessThanOrEqualTo(9);
        assertThat(details.moves()).extracting(SessionDetails.MoveRecord::moveNumber)
                .containsExactlyElementsOf(IntStream.rangeClosed(1, details.moves().size()).boxed().toList());
        assertThat(details.moves()).extracting(SessionDetails.MoveRecord::player)
                .startsWith(Player.X, Player.O);
        assertThat(details.moves().getLast().gameStatus()).isEqualTo(details.game().status());
        assertThat(details.moves().subList(0, details.moves().size() - 1))
                .allSatisfy(move -> assertThat(move.gameStatus()).isEqualTo(GameStatus.IN_PROGRESS));
        assertThat(firstEngineCallAfterSimulate(id)).isEqualTo("GET /games/" + id);
        assertThat(details.failureReason()).isNull();
    }

    @Test
    void engineGoingDownMidGameFailsTheSession() throws Exception {
        UUID id = createSession();
        CountDownLatch gate = startAndHoldAtMove(id, 3);
        engine.goDown();
        gate.countDown();

        SessionDetails details = awaitState(id, SessionState.FAILED);

        assertThat(details.failureReason()).isEqualTo("ENGINE_UNAVAILABLE");
        assertThat(details.moves()).hasSize(3);
        assertThat(details.game().status()).isEqualTo(GameStatus.IN_PROGRESS);
    }

    @Test
    void aMoveThatLandedDespiteAFailedResponseIsNotSubmittedTwice() {
        UUID id = createSession();
        engine.failNextMove(MoveFault.RESET_AFTER_APPLY);
        simulate(id);

        SessionDetails details = awaitState(id, SessionState.FINISHED);

        assertThat(engine.movesApplied()).isEqualTo(details.moves().size());
        wireMock.verify(details.moves().size(), postRequestedFor(urlPathMatching("/games/.*/moves")));
    }

    @Test
    void aMoveThatDidNotLandIsRetried() {
        UUID id = createSession();
        engine.failNextMove(MoveFault.RESET_BEFORE_APPLY);
        simulate(id);

        SessionDetails details = awaitState(id, SessionState.FINISHED);

        assertThat(engine.movesApplied()).isEqualTo(details.moves().size());
        wireMock.verify(details.moves().size() + 1, postRequestedFor(urlPathMatching("/games/.*/moves")));
    }

    @Test
    void aTimedOutMoveIsRecoveredFromTheEngineState() {
        UUID id = createSession();
        engine.failNextMove(MoveFault.DELAY_AFTER_APPLY);
        simulate(id);

        SessionDetails details = awaitState(id, SessionState.FINISHED);

        assertThat(engine.movesApplied()).isEqualTo(details.moves().size());
    }

    @Test
    void recoveryWorksWhenTheAmbiguousMoveEndedTheGame() {
        UUID id = createSession();
        engine.failWhenGameEnds();
        simulate(id);

        SessionDetails details = awaitState(id, SessionState.FINISHED);

        assertThat(details.game().status().isTerminal()).isTrue();
        assertThat(details.moves().getLast().gameStatus()).isEqualTo(details.game().status());
        assertThat(engine.movesApplied()).isEqualTo(details.moves().size());
    }

    @Test
    void anEngineRejectionFailsTheSessionWithTheEngineCode() {
        UUID id = createSession();
        engine.failNextMove(MoveFault.WRONG_TURN);
        simulate(id);

        SessionDetails details = awaitState(id, SessionState.FAILED);

        assertThat(details.failureReason()).isEqualTo("WRONG_TURN");
    }

    @Test
    void aPersistenceFailureInsideTheLoopFailsTheSessionInsteadOfLeavingItRunning() {
        UUID id = createSession();
        moveRecords.save(new MoveRecordEntity(id, 1, Player.X, 0, GameStatus.IN_PROGRESS, Instant.now()));
        simulate(id);

        SessionDetails details = awaitState(id, SessionState.FAILED);

        assertThat(details.failureReason()).isEqualTo("INTERNAL_ERROR");
    }

    @Test
    void concurrentSimulateCallsStartExactlyOneLoop() throws Exception {
        UUID id = createSession();
        CountDownLatch start = new CountDownLatch(1);
        List<Future<HttpStatus>> outcomes;
        try (ExecutorService pool = Executors.newFixedThreadPool(2)) {
            outcomes = IntStream.range(0, 2).mapToObj(ignored -> pool.submit(() -> {
                start.await();
                return HttpStatus.valueOf(simulate(id).getStatusCode().value());
            })).toList();
            start.countDown();
        }

        List<HttpStatus> statuses = List.of(outcomes.get(0).get(), outcomes.get(1).get());
        SessionDetails details = awaitState(id, SessionState.FINISHED);

        assertThat(statuses).containsExactlyInAnyOrder(HttpStatus.ACCEPTED, HttpStatus.CONFLICT);
        assertThat(engine.movesApplied()).isEqualTo(details.moves().size());
    }

    private String firstEngineCallAfterSimulate(UUID id) {
        List<ServeEvent> newestFirst = wireMock.getAllServeEvents();
        ServeEvent first = newestFirst.reversed().stream()
                .filter(event -> !event.getRequest().getUrl().equals("/games"))
                .findFirst().orElseThrow();
        return first.getRequest().getMethod().getName() + " " + first.getRequest().getUrl();
    }
}
