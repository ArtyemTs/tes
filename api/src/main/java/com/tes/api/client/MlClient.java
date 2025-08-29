package com.tes.api.client;

import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.tcp.TcpClient;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Component
public class MlClient {
    private final WebClient wc;

    public MlClient(@Value("${tes.ml.base-url:http://ml:5000}") String baseUrl,
                    @Value("${tes.ml.timeouts.connect:1s}") Duration connectTimeout,
                    @Value("${tes.ml.timeouts.read:3s}") Duration readTimeout) {
        TcpClient tcp = TcpClient.create()
                .option(io.netty.channel.ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) connectTimeout.toMillis())
                .doOnConnected(conn -> conn
                        .addHandlerLast(new io.netty.handler.timeout.ReadTimeoutHandler(readTimeout.toMillis(), TimeUnit.MILLISECONDS))
                        .addHandlerLast(new io.netty.handler.timeout.WriteTimeoutHandler(readTimeout.toMillis(), TimeUnit.MILLISECONDS)));
        this.wc = WebClient.builder()
                .baseUrl(baseUrl)
                .clientConnector(new org.springframework.http.client.reactive.ReactorClientHttpConnector(HttpClient.from(tcp)))
                .exchangeStrategies(ExchangeStrategies.builder()
                        .codecs(cfg -> cfg.defaultCodecs().maxInMemorySize(256 * 1024))
                        .build())
                .build();
    }

    @CircuitBreaker(name = "ml")
    @TimeLimiter(name = "ml")
    @Bulkhead(name = "ml")
    public CompletableFuture<Map> recommendAsync(Map<String, Object> payload) {
        return wc.post()
                .uri("/recommend")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(Map.class)
                .timeout(Duration.ofSeconds(3))
                .toFuture();
    }
}
