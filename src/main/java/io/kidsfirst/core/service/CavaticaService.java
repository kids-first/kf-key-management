package io.kidsfirst.core.service;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.val;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Service
public class CavaticaService {

    private final WebClient client;

    public CavaticaService(@Value("${application.cavatica_root}") String cavaticaRoot) {
        val httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                .responseTimeout(Duration.ofMillis(5000))
                .doOnConnected(conn ->
                        conn.addHandlerLast(new ReadTimeoutHandler(10000, TimeUnit.MILLISECONDS))
                                .addHandlerLast(new WriteTimeoutHandler(5000, TimeUnit.MILLISECONDS)));

        this.client = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .baseUrl(cavaticaRoot)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public Mono<String> sendCavaticaRequest(String cavaticaKey, String path, String method, String body) {
        return client
                .method(HttpMethod.valueOf(method))
                .uri(path).bodyValue(body)
                .header("X-SBG-Auth-Token", cavaticaKey)
                .exchangeToMono(r -> {
                    if (r.statusCode().is2xxSuccessful()) {
                        return r.bodyToMono(String.class);
                    } else {
                        return r.createException()
                                .flatMap(Mono::error);
                    }
                });
    }
}
