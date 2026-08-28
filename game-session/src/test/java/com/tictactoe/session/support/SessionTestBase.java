package com.tictactoe.session.support;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.tictactoe.session.domain.SessionState;
import com.tictactoe.session.simulation.SessionDetails;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class SessionTestBase {

    protected static final FakeEngine engine = new FakeEngine();

    /**
     * One server for the whole JVM: the Spring context is cached across test classes and
     * keeps the Engine URL it was started with, so the server must outlive any one class.
     */
    protected static final WireMockServer wireMock = new WireMockServer(wireMockConfig().dynamicPort().extensions(engine));

    static {
        wireMock.start();
    }

    @Autowired
    protected TestRestTemplate http;

    @DynamicPropertySource
    static void engineUrl(DynamicPropertyRegistry registry) {
        registry.add("engine.base-url", wireMock::baseUrl);
    }

    @BeforeEach
    void resetEngine() {
        engine.reset();
        wireMock.resetRequests();
        wireMock.stubFor(any(urlPathMatching("/games.*")).willReturn(aResponse().withTransformers(FakeEngine.NAME)));
    }

    protected UUID createSession() {
        ResponseEntity<SessionDetails> response = http.postForEntity("/sessions", null, SessionDetails.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return response.getBody().sessionId();
    }

    protected ResponseEntity<String> simulate(UUID sessionId) {
        return http.postForEntity("/sessions/" + sessionId + "/simulate", null, String.class);
    }

    protected SessionDetails details(UUID sessionId) {
        return http.getForObject("/sessions/" + sessionId, SessionDetails.class);
    }

    protected SessionDetails awaitDetails(UUID sessionId, Predicate<SessionDetails> condition) {
        return await().atMost(Duration.ofSeconds(10)).until(() -> details(sessionId), condition);
    }

    protected SessionDetails awaitState(UUID sessionId, SessionState expected) {
        return awaitDetails(sessionId, details -> details.state() == expected);
    }

    protected SessionDetails awaitTerminal(UUID sessionId) {
        return awaitDetails(sessionId, details -> details.state() == SessionState.FINISHED || details.state() == SessionState.FAILED);
    }

    protected SessionDetails awaitMoves(UUID sessionId, int atLeast) {
        return awaitDetails(sessionId, details -> details.moves().size() >= atLeast);
    }

    /** Starts the simulation and freezes it right before the given move; returns the gate that releases it. */
    protected CountDownLatch startAndHoldAtMove(UUID sessionId, int moveNumber) throws InterruptedException {
        CountDownLatch gate = new CountDownLatch(1);
        CountDownLatch reached = new CountDownLatch(1);
        engine.holdBeforeMove(moveNumber, gate, reached);
        simulate(sessionId);
        assertThat(reached.await(10, TimeUnit.SECONDS)).isTrue();
        return gate;
    }

    protected String eventsUrl(UUID sessionId) {
        return http.getRootUri() + "/sessions/" + sessionId + "/events";
    }
}
