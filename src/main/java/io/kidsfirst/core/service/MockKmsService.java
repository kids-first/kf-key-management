package io.kidsfirst.core.service;

import io.kidsfirst.core.service.KmsService;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@Profile("dev")
public class MockKmsService implements KmsService {

    @Override
    public Mono<String> encrypt(String original) {
        return Mono.just("encrypted_" + original);
    }

    @Override
    public Mono<String> decrypt(String cipher) {
        return Mono.just(cipher.replaceFirst("encrypted_", ""));
    }
}
