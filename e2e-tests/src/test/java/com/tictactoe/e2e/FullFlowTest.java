package com.tictactoe.e2e;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Drives the stack the way a browser does: every public call goes through the
 * frontend's nginx under {@code /api}. The engine is only consulted directly to prove
 * the session's snapshot matches the source of truth.
 */
class FullFlowTest {

    private static final Set<String> TERMINAL = Set.of("X_WON", "O_WON", "DRAW");
    private static final Duration GAME = Duration.ofSeconds(30);

    @Test
    void aSessionPlaysItselfToTheEndThroughThePublicEntryPoint() {
        String api = Stack.publicApi();

        Http.Reply created = Http.post(api + "/sessions");
        assertThat(created.status()).isEqualTo(201);
        String sessionId = created.body().get("sessionId").asText();
        assertThat(Http.post(api + "/sessions/" + sessionId + "/simulate").status()).isEqualTo(202);

        JsonNode session = awaitFinished(api, sessionId);

        assertThat(session.get("game").get("status").asText()).isIn(TERMINAL);
        List<Integer> moveNumbers = session.get("moves").valueStream().map(move -> move.get("moveNumber").asInt()).toList();
        assertThat(moveNumbers).isNotEmpty().hasSizeLessThanOrEqualTo(9)
                .containsExactlyElementsOf(IntStream.rangeClosed(1, moveNumbers.size()).boxed().toList());
        assertThat(session.get("failureReason").isNull()).isTrue();
    }

    @Test
    void theSessionSnapshotMatchesTheEngineStateOfTruth() {
        String api = Stack.publicApi();
        String sessionId = Http.post(api + "/sessions").body().get("sessionId").asText();
        Http.post(api + "/sessions/" + sessionId + "/simulate");
        JsonNode session = awaitFinished(api, sessionId);

        JsonNode engineGame = Http.get(Stack.url(Stack.ENGINE, 8081) + "/games/" + sessionId).body();

        assertThat(session.get("game").get("board")).isEqualTo(engineGame.get("board"));
        assertThat(session.get("game").get("status")).isEqualTo(engineGame.get("status"));
        assertThat(session.get("game").get("winningLine")).isEqualTo(engineGame.get("winningLine"));
    }

    @Test
    void progressStreamsThroughNginxWhileTheGameIsStillRunning() throws InterruptedException {
        String api = Stack.publicApi();
        String sessionId = Http.post(api + "/sessions").body().get("sessionId").asText();

        try (SseClient stream = new SseClient(api + "/sessions/" + sessionId + "/events")) {
            Http.post(api + "/sessions/" + sessionId + "/simulate");
            SseClient.Event first = stream.next(GAME);
            String stateWhenFirstMoveArrived = Http.get(api + "/sessions/" + sessionId).body().get("state").asText();
            SseClient.Event finished = stream.nextNamed("finished", GAME);

            assertThat(first.name()).isEqualTo("move");
            assertThat(first.data()).contains("\"moveNumber\":1");
            assertThat(stateWhenFirstMoveArrived).isEqualTo("RUNNING");
            assertThat(finished.data()).contains("\"board\":[");
        }
    }

    private static JsonNode awaitFinished(String api, String sessionId) {
        return await().atMost(GAME).pollInterval(Duration.ofMillis(250))
                .until(() -> Http.get(api + "/sessions/" + sessionId).body(),
                        session -> session.get("state").asText().equals("FINISHED"));
    }
}
