package com.tictactoe.engine.api;

import com.tictactoe.engine.domain.Symbol;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GameConcurrencyTest {

    private static final int THREADS = 20;

    @Autowired
    private TestRestTemplate http;

    /**
     * Twenty simultaneous X moves on a fresh game: some target the same cell, the rest
     * distinct free cells. Exactly one may land; every other one must be refused as
     * either out of turn or on an occupied cell, and the board must hold a single X.
     */
    @Test
    void onlyOneOfManySimultaneousMovesIsApplied() throws Exception {
        UUID id = UUID.randomUUID();
        http.postForEntity("/games", Map.of("gameId", id), GameStateResponse.class);
        CountDownLatch start = new CountDownLatch(1);

        List<Future<HttpStatus>> outcomes;
        try (ExecutorService pool = Executors.newFixedThreadPool(THREADS)) {
            outcomes = IntStream.range(0, THREADS)
                    .mapToObj(thread -> pool.submit(() -> {
                        start.await();
                        return submitMove(id, thread % 9);
                    }))
                    .toList();
            start.countDown();
        }

        List<HttpStatus> statuses = outcomes.stream().map(GameConcurrencyTest::result).toList();
        assertThat(statuses).filteredOn(HttpStatus.OK::equals).hasSize(1);
        assertThat(statuses).filteredOn(HttpStatus.CONFLICT::equals).hasSize(THREADS - 1);

        List<Symbol> board = http.getForEntity("/games/" + id, GameStateResponse.class).getBody().board();
        assertThat(board.stream().filter(Objects::nonNull)).containsExactly(Symbol.X);
    }

    private HttpStatus submitMove(UUID id, int position) {
        ResponseEntity<String> response = http.postForEntity("/games/" + id + "/moves",
                Map.of("player", "X", "position", position), String.class);
        return HttpStatus.valueOf(response.getStatusCode().value());
    }

    private static HttpStatus result(Future<HttpStatus> future) {
        try {
            return future.get();
        } catch (Exception ex) {
            throw new AssertionError("Move submission failed", ex);
        }
    }
}
