package com.tictactoe.session.config;

import com.tictactoe.session.engine.EngineRejectedException;
import com.tictactoe.session.web.RequestIdFilter;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
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

    /** Fixed address (compose without discovery, local runs, tests). */
    @Bean
    @ConditionalOnProperty(name = "engine.discovery", havingValue = "false", matchIfMissing = true)
    public RestClient engineRestClient(EngineProperties properties) {
        return configure(RestClient.builder(), properties);
    }

    /** Service id resolved through the registry; the builder gets the load-balancer interceptor. */
    @Bean
    @ConditionalOnProperty(name = "engine.discovery", havingValue = "true")
    public RestClient discoveredEngineRestClient(@LoadBalanced RestClient.Builder builder, EngineProperties properties) {
        return configure(builder, properties);
    }

    @Bean
    @LoadBalanced
    @ConditionalOnProperty(name = "engine.discovery", havingValue = "true")
    public RestClient.Builder loadBalancedRestClientBuilder() {
        return RestClient.builder();
    }

    /**
     * Every 4xx becomes an {@link EngineRejectedException} carrying the engine's error
     * code, so call sites only deal with transport failures.
     */
    private static RestClient configure(RestClient.Builder builder, EngineProperties properties) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.connectTimeout())
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(properties.readTimeout());
        return builder
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
