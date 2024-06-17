package io.kidsfirst.core.service;

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
    public Mono<String> compressAndEncrypt(String original) {
        return Mono.just("encrypted_compressed_" + original);
    }

    @Override
    public Mono<String> decrypt(String cipher) {
        return Mono.just(cipher.replaceFirst("encrypted_", ""));
    }

    @Override
    public Mono<String> decryptAndDecompress(String cipher) {
        return Mono.just(cipher.replaceFirst("decompressed_encrypted_", ""));
    }
}
