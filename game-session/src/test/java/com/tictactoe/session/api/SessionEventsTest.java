package com.tictactoe.session.api;

import com.tictactoe.session.domain.SessionState;
import com.tictactoe.session.simulation.SessionDetails;
import com.tictactoe.session.support.FakeEngine.MoveFault;
import com.tictactoe.session.support.SessionTestBase;
import com.tictactoe.session.support.SseTestClient;
import com.tictactoe.session.support.SseTestClient.Event;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class SessionEventsTest extends SessionTestBase {

    private static final Duration WAIT = Duration.ofSeconds(10);

    @Test
    void streamsEveryMoveInOrderThenASelfSufficientFinishedEvent() throws Exception {
        UUID id = createSession();
        try (SseTestClient stream = new SseTestClient(eventsUrl(id))) {
            assertThat(stream.awaitConnected(WAIT)).isEqualTo(200);
            simulate(id);

            List<Event> events = stream.collectUntilClosed(WAIT);

            List<Event> moves = events.stream().filter(event -> event.name().equals("move")).toList();
            assertThat(moves).extracting(Event::id)
                    .containsExactlyElementsOf(IntStream.rangeClosed(1, moves.size()).mapToObj(String::valueOf).toList());
            assertThat(moves.getFirst().data()).contains("\"moveNumber\":1", "\"player\":\"X\"", "\"board\":[");
            Event finished = events.getLast();
            assertThat(finished.name()).isEqualTo("finished");
            assertThat(finished.data()).contains("\"status\":", "\"board\":[");
        }
    }

    @Test
    void subscribingMidGameReplaysHistoryThenContinuesLiveWithoutGaps() throws Exception {
        UUID id = createSession();
        CountDownLatch gate = startAndHoldAtMove(id, 4);
        awaitMoves(id, 3);

        try (SseTestClient stream = new SseTestClient(eventsUrl(id))) {
            assertThat(stream.awaitConnected(WAIT)).isEqualTo(200);
            Event first = stream.next(WAIT);
            Event second = stream.next(WAIT);
            Event third = stream.next(WAIT);
            gate.countDown();
            List<Event> rest = stream.collectUntilClosed(WAIT);

            assertThat(List.of(first, second, third)).extracting(Event::id).containsExactly("1", "2", "3");
            List<Event> all = new ArrayList<>(List.of(first, second, third));
            all.addAll(rest);
            List<Integer> allMoveNumbers = all.stream()
                    .filter(event -> event.name().equals("move"))
                    .map(event -> Integer.parseInt(event.id()))
                    .distinct().sorted().toList();
            int total = details(id).moves().size();
            assertThat(allMoveNumbers).containsExactlyElementsOf(IntStream.rangeClosed(1, total).boxed().toList());
            assertThat(rest.getLast().name()).isEqualTo("finished");
        }
    }

    @Test
    void subscribingToAFinishedSessionReplaysEverythingAndCompletes() throws Exception {
        UUID id = createSession();
        simulate(id);
        SessionDetails details = awaitState(id, SessionState.FINISHED);

        try (SseTestClient stream = new SseTestClient(eventsUrl(id))) {
            List<Event> events = stream.collectUntilClosed(WAIT);

            assertThat(events).hasSize(details.moves().size() + 1);
            assertThat(events.getLast().name()).isEqualTo("finished");
        }
    }

    @Test
    void subscribingToAFailedSessionReplaysThenSendsTheFailure() throws Exception {
        UUID id = createSession();
        engine.failNextMove(MoveFault.WRONG_TURN);
        simulate(id);
        awaitState(id, SessionState.FAILED);

        try (SseTestClient stream = new SseTestClient(eventsUrl(id))) {
            List<Event> events = stream.collectUntilClosed(WAIT);

            assertThat(events.getLast().name()).isEqualTo("failed");
            assertThat(events.getLast().data()).contains("\"code\":\"WRONG_TURN\"");
        }
    }

    @Test
    void liveFailureIsPushedAsAFailedEvent() throws Exception {
        UUID id = createSession();
        try (SseTestClient stream = new SseTestClient(eventsUrl(id))) {
            stream.awaitConnected(WAIT);
            engine.goDown();
            simulate(id);

            List<Event> events = stream.collectUntilClosed(WAIT);

            assertThat(events).hasSize(1);
            assertThat(events.getFirst().name()).isEqualTo("failed");
            assertThat(events.getFirst().data()).contains("ENGINE_UNAVAILABLE");
            assertThat(details(id).state()).isEqualTo(SessionState.FAILED);
        }
    }

    @Test
    void subscribingToACreatedSessionKeepsTheStreamOpenUntilTheSimulationStarts() throws Exception {
        UUID id = createSession();
        try (SseTestClient stream = new SseTestClient(eventsUrl(id))) {
            assertThat(stream.awaitConnected(WAIT)).isEqualTo(200);
            assertThat(stream.poll(Duration.ofMillis(300))).isNull();
            assertThat(stream.isClosed()).isFalse();

            simulate(id);
            List<Event> events = stream.collectUntilClosed(WAIT);

            assertThat(events.getFirst().name()).isEqualTo("move");
            assertThat(events.getLast().name()).isEqualTo("finished");
        }
    }

    @Test
    void unknownSessionStreamIsNotFound() throws Exception {
        try (SseTestClient stream = new SseTestClient(eventsUrl(UUID.randomUUID()))) {
            assertThat(stream.awaitConnected(WAIT)).isEqualTo(404);
        }
    }
}
