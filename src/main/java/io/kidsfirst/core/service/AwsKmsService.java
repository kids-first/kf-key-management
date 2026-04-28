package io.kidsfirst.core.service;

import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.kms.KmsAsyncClient;
import software.amazon.awssdk.services.kms.model.DecryptRequest;
import software.amazon.awssdk.services.kms.model.EncryptRequest;
import software.amazon.awssdk.services.kms.model.KmsException;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Slf4j
@Service
@Profile("!dev | localstack")
public class AwsKmsService implements KmsService {

    private final String keyId;
    private final KmsAsyncClient kms;

    public AwsKmsService(@Value("${application.kms}") String keyId, KmsAsyncClient kms) {
        this.keyId = keyId;
        this.kms = kms;
    }

    public Mono<String> encrypt(String original) {
        val request = EncryptRequest.builder()
                .keyId(keyId)
                .plaintext(SdkBytes.fromByteArray(original.getBytes(StandardCharsets.ISO_8859_1)))
                .build();
        return Mono.fromFuture(kms.encrypt(request))
                .map(result -> new String(result.ciphertextBlob().asByteArray(), StandardCharsets.ISO_8859_1))
                .onErrorResume(KmsException.class, e -> {
                    log.error("KmsException occurs when encrypting with message {}", e.getMessage());
                    return Mono.empty();
                });
    }

    @Override
    public Mono<String> compressAndEncrypt(String original) {
        String compressedOriginal = StringCompressService.compress(original);
        return encrypt(compressedOriginal);
    }

    public Mono<String> decrypt(String cipher) {
        val request = DecryptRequest.builder()
                .ciphertextBlob(SdkBytes.fromByteArray(cipher.getBytes(StandardCharsets.ISO_8859_1)))
                .build();
        return Mono.fromFuture(kms.decrypt(request))
                .map(result -> new String(result.plaintext().asByteArray(), StandardCharsets.ISO_8859_1));
    }

    @Override
    public Mono<String> decryptAndDecompress(String cipher) {
        return decrypt(cipher).map(StringCompressService::decompress);
    }
}
