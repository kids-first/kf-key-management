package io.kidsfirst.core.service;

import reactor.core.publisher.Mono;

public interface KmsService {

    Mono<String> encrypt(String original);
    Mono<String> compressAndEncrypt(String original);

    Mono<String> decrypt(String cipher);
    Mono<String> decryptAndDecompress(String cipher);
}
