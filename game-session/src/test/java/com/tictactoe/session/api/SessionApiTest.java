package com.tictactoe.session.api;

import com.tictactoe.session.domain.Board;
import com.tictactoe.session.domain.SessionState;
import com.tictactoe.session.persistence.SessionRepository;
import com.tictactoe.session.simulation.SessionDetails;
import com.tictactoe.session.support.SessionTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.assertj.core.api.Assertions.assertThat;

class SessionApiTest extends SessionTestBase {

    @Autowired
    private SessionRepository sessions;

    @Test
    void createReturnsCreatedSessionAndCreatesTheEngineGame() {
        ResponseEntity<SessionDetails> response = http.postForEntity("/sessions", null, SessionDetails.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        SessionDetails body = response.getBody();
        assertThat(body.state()).isEqualTo(SessionState.CREATED);
        assertThat(body.moves()).isEmpty();
        assertThat(body.game().board()).hasSize(9).containsOnlyNulls();
        wireMock.verify(1, postRequestedFor(urlEqualTo("/games")));
        assertThat(engine.boardOf(body.sessionId().toString())).isEqualTo(Board.empty().encode());
    }

    @Test
    void engineDownOnCreateIsServiceUnavailableAndLeavesNoSession() {
        long before = sessions.count();
        engine.goDown();

        ResponseEntity<ErrorResponse> response = http.postForEntity("/sessions", null, ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody().code()).isEqualTo("ENGINE_UNAVAILABLE");
        assertThat(sessions.count()).isEqualTo(before);
        wireMock.verify(3, postRequestedFor(urlEqualTo("/games")));
    }

    @Test
    void createRetryAnsweredWithAlreadyExistsIsTreatedAsSuccess() {
        engine.failCreateAfterApply();

        ResponseEntity<SessionDetails> response = http.postForEntity("/sessions", null, SessionDetails.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        wireMock.verify(2, postRequestedFor(urlEqualTo("/games")));
        assertThat(details(response.getBody().sessionId()).state()).isEqualTo(SessionState.CREATED);
    }

    @Test
    void unknownSessionIsNotFound() {
        UUID unknown = UUID.randomUUID();

        ResponseEntity<ErrorResponse> get = http.getForEntity("/sessions/" + unknown, ErrorResponse.class);
        ResponseEntity<ErrorResponse> simulate = http.postForEntity("/sessions/" + unknown + "/simulate", null, ErrorResponse.class);

        assertThat(get.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(get.getBody().code()).isEqualTo("SESSION_NOT_FOUND");
        assertThat(get.getBody().timestamp()).isNotNull();
        assertThat(simulate.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void simulateCanOnlyStartOnce() {
        UUID id = createSession();

        assertThat(simulate(id).getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        ResponseEntity<ErrorResponse> second = http.postForEntity("/sessions/" + id + "/simulate", null, ErrorResponse.class);
        awaitTerminal(id);
        ResponseEntity<ErrorResponse> afterFinish = http.postForEntity("/sessions/" + id + "/simulate", null, ErrorResponse.class);

        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(second.getBody().code()).isEqualTo("SIMULATION_ALREADY_STARTED");
        assertThat(afterFinish.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(afterFinish.getBody().code()).isEqualTo("SIMULATION_ALREADY_STARTED");
    }

    @Test
    void detailsAreServedFromTheSnapshotWithoutCallingTheEngine() {
        UUID id = createSession();
        simulate(id);
        awaitState(id, SessionState.FINISHED);
        wireMock.resetRequests();

        SessionDetails details = details(id);

        assertThat(new Board(details.game().board()).encode()).isEqualTo(engine.boardOf(id.toString()));
        wireMock.verify(0, getRequestedFor(urlPathMatching("/games/.*")));
    }

    @Test
    void requestIdIsEchoed() {
        ResponseEntity<SessionDetails> response = http.postForEntity("/sessions", null, SessionDetails.class);

        assertThat(response.getHeaders().getFirst("X-Request-Id")).isNotBlank();
        wireMock.verify(postRequestedFor(urlEqualTo("/games"))
                .withHeader("X-Request-Id", equalTo(response.getHeaders().getFirst("X-Request-Id"))));
    }
}
