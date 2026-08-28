package com.tictactoe.engine.api;

import com.tictactoe.engine.domain.GameStatus;
import com.tictactoe.engine.domain.Symbol;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GameApiTest {

    @Autowired
    private TestRestTemplate http;

    @Test
    void createReturnsEmptyBoardWithXToMove() {
        UUID id = UUID.randomUUID();

        ResponseEntity<GameStateResponse> response = create(id);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().gameId()).isEqualTo(id);
        assertThat(response.getBody().board()).containsOnlyNulls().hasSize(9);
        assertThat(response.getBody().nextPlayer()).isEqualTo(Symbol.X);
        assertThat(response.getBody().status()).isEqualTo(GameStatus.IN_PROGRESS);
    }

    @Test
    void duplicateCreateIsAConflict() {
        UUID id = UUID.randomUUID();
        create(id);

        ResponseEntity<ErrorResponse> response = http.postForEntity("/games", Map.of("gameId", id), ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().code()).isEqualTo("GAME_ALREADY_EXISTS");
    }

    @Test
    void unknownGameIsNotFoundWithErrorContract() {
        UUID id = UUID.randomUUID();

        ResponseEntity<ErrorResponse> get = http.getForEntity("/games/" + id, ErrorResponse.class);
        ResponseEntity<ErrorResponse> move = http.postForEntity("/games/" + id + "/moves",
                Map.of("player", "X", "position", 0), ErrorResponse.class);

        assertThat(get.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(get.getBody().code()).isEqualTo("GAME_NOT_FOUND");
        assertThat(get.getBody().message()).isNotBlank();
        assertThat(get.getBody().timestamp()).isNotNull();
        assertThat(move.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(move.getBody().code()).isEqualTo("GAME_NOT_FOUND");
    }

    @Test
    void fullGameOverHttpEndsWithAWin() {
        UUID id = UUID.randomUUID();
        create(id);

        move(id, Symbol.X, 0);
        move(id, Symbol.O, 3);
        move(id, Symbol.X, 1);
        move(id, Symbol.O, 4);
        ResponseEntity<GameStateResponse> last = move(id, Symbol.X, 2);

        assertThat(last.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(last.getBody().status()).isEqualTo(GameStatus.X_WON);
        assertThat(last.getBody().winningLine()).isEqualTo(List.of(0, 1, 2));
        assertThat(last.getBody().nextPlayer()).isNull();
        assertThat(http.getForEntity("/games/" + id, GameStateResponse.class).getBody().status())
                .isEqualTo(GameStatus.X_WON);
    }

    @Test
    void illegalMovesAreConflictsWithExactCodes() {
        UUID id = UUID.randomUUID();
        create(id);
        move(id, Symbol.X, 4);

        assertThat(moveError(id, Symbol.X, 0).code()).isEqualTo("WRONG_TURN");
        assertThat(moveError(id, Symbol.O, 4).code()).isEqualTo("CELL_OCCUPIED");

        move(id, Symbol.O, 3);
        move(id, Symbol.X, 0);
        move(id, Symbol.O, 5);
        move(id, Symbol.X, 8);
        assertThat(moveError(id, Symbol.O, 1).code()).isEqualTo("GAME_FINISHED");
    }

    @Test
    void malformedRequestsAreValidationErrors() {
        UUID id = UUID.randomUUID();
        create(id);

        ResponseEntity<ErrorResponse> outOfRange = http.postForEntity("/games/" + id + "/moves",
                Map.of("player", "X", "position", 9), ErrorResponse.class);
        ResponseEntity<ErrorResponse> badSymbol = http.postForEntity("/games/" + id + "/moves",
                Map.of("player", "Z", "position", 1), ErrorResponse.class);
        ResponseEntity<ErrorResponse> notJson = http.postForEntity("/games/" + id + "/moves",
                new HttpEntity<>("not json", jsonHeaders()), ErrorResponse.class);
        ResponseEntity<ErrorResponse> badId = http.getForEntity("/games/not-a-uuid", ErrorResponse.class);

        assertThat(outOfRange.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(outOfRange.getBody().code()).isEqualTo("VALIDATION_ERROR");
        assertThat(badSymbol.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(notJson.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(badId.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(notJson.getBody().message()).doesNotContain("Exception");
    }

    @Test
    void requestIdIsEchoedWhenSupplied() {
        HttpHeaders headers = jsonHeaders();
        headers.set("X-Request-Id", "trace-123");

        ResponseEntity<GameStateResponse> response = http.postForEntity("/games",
                new HttpEntity<>(Map.of("gameId", UUID.randomUUID()), headers), GameStateResponse.class);

        assertThat(response.getHeaders().getFirst("X-Request-Id")).isEqualTo("trace-123");
        assertThat(http.getForEntity("/games/" + response.getBody().gameId(), GameStateResponse.class)
                .getHeaders().getFirst("X-Request-Id")).isNotBlank();
    }

    private ResponseEntity<GameStateResponse> create(UUID id) {
        return http.postForEntity("/games", Map.of("gameId", id), GameStateResponse.class);
    }

    private ResponseEntity<GameStateResponse> move(UUID id, Symbol player, int position) {
        ResponseEntity<GameStateResponse> response = http.postForEntity("/games/" + id + "/moves",
                Map.of("player", player, "position", position), GameStateResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return response;
    }

    private ErrorResponse moveError(UUID id, Symbol player, int position) {
        ResponseEntity<ErrorResponse> response = http.postForEntity("/games/" + id + "/moves",
                Map.of("player", player, "position", position), ErrorResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        return response.getBody();
    }

    private static HttpHeaders jsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}
