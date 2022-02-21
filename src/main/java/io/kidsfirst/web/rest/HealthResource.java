package io.kidsfirst.web.rest;

import io.kidsfirst.core.model.HealthStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/status")
public class HealthResource {

    @GetMapping()
    public Mono<HealthStatus> status() throws IllegalArgumentException {
        return Mono.just(new HealthStatus("OK"));
    }
}
