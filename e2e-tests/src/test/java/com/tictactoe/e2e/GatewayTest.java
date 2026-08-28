package com.tictactoe.e2e;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Proves the discovery-based topology: every service is registered, the browser path
 * (nginx -> gateway -> session) strips {@code /api} exactly once, and the engine is not
 * reachable through the gateway.
 */
class GatewayTest {

    private static final Duration WAIT = Duration.ofSeconds(60);

    @Test
    void everyServiceRegistersWithDiscovery() {
        String registry = Stack.url(Stack.DISCOVERY, 8761) + "/eureka/apps";

        List<String> registered = await().atMost(WAIT).pollInterval(Duration.ofSeconds(1))
                .until(() -> applicationNames(registry),
                        names -> names.containsAll(List.of("GAME-ENGINE", "GAME-SESSION", "GATEWAY")));

        assertThat(registered).contains("GAME-ENGINE", "GAME-SESSION", "GATEWAY");
    }

    @Test
    void theBrowserPathReachesTheSessionServiceWithThePrefixStrippedOnce() {
        String api = Stack.publicApi();

        Http.Reply created = Http.post(api + "/sessions");
        String sessionId = created.body().get("sessionId").asText();
        Http.Reply fetched = Http.get(api + "/sessions/" + sessionId);
        Http.Reply engineThroughGateway = Http.get(api + "/games/" + sessionId);

        assertThat(created.status()).isEqualTo(201);
        assertThat(fetched.status()).isEqualTo(200);
        assertThat(engineThroughGateway.status()).isEqualTo(404);
    }

    private static List<String> applicationNames(String registryUrl) {
        JsonNode apps;
        try {
            apps = Http.get(registryUrl).body().path("applications").path("application");
        } catch (IllegalStateException notYet) {
            return List.of();
        }
        return apps.valueStream().map(app -> app.get("name").asText()).toList();
    }
}
