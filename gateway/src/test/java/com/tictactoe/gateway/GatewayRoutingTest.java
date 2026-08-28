package com.tictactoe.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.RouteLocator;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {"eureka.client.enabled=false", "spring.cloud.discovery.enabled=false"})
class GatewayRoutingTest {

    @Autowired
    private RouteLocator routes;

    @Test
    void onlyTheSessionServiceIsRoutedAndThePrefixIsStripped() {
        var all = routes.getRoutes().collectList().block();

        assertThat(all).hasSize(1);
        assertThat(all.getFirst().getId()).isEqualTo("sessions");
        assertThat(all.getFirst().getUri().toString()).isEqualTo("lb://game-session");
        assertThat(all.getFirst().getFilters()).hasSize(1);
    }
}
