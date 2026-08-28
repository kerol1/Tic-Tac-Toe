package com.tictactoe.session.config;

import com.tictactoe.session.engine.EngineRejectedException;
import com.tictactoe.session.web.RequestIdFilter;
import org.slf4j.MDC;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;

@Configuration
@EnableConfigurationProperties({EngineProperties.class, SimulationProperties.class})
public class EngineClientConfig {

    /**
     * Every 4xx becomes an {@link EngineRejectedException} carrying the engine's error
     * code, so call sites only deal with transport failures.
     */
    @Bean
    public RestClient engineRestClient(EngineProperties properties) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.connectTimeout())
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(properties.readTimeout());
        return RestClient.builder()
                .baseUrl(properties.baseUrl())
                .requestFactory(requestFactory)
                .requestInterceptor(propagateRequestId())
                .defaultStatusHandler(HttpStatusCode::is4xxClientError, (request, response) -> {
                    throw EngineRejectedException.from(response);
                })
                .build();
    }

    private static ClientHttpRequestInterceptor propagateRequestId() {
        return (request, body, execution) -> {
            String requestId = MDC.get(RequestIdFilter.MDC_KEY);
            if (requestId != null) {
                request.getHeaders().set(RequestIdFilter.HEADER, requestId);
            }
            return execution.execute(request, body);
        };
    }
}
