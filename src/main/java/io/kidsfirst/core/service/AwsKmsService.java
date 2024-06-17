package io.kidsfirst.core.service;

import com.amazonaws.services.kms.AWSKMS;
import com.amazonaws.services.kms.AWSKMSAsyncClient;
import com.amazonaws.services.kms.model.AWSKMSException;
import com.amazonaws.services.kms.model.DecryptRequest;
import com.amazonaws.services.kms.model.EncryptRequest;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

@Slf4j
@Service
@Profile("!dev")
public class AwsKmsService implements KmsService {

    private final String keyId;
    private final AWSKMS kms;

    public AwsKmsService(@Value("${application.kms}") String keyId) {
        this.keyId = keyId;
        this.kms = AWSKMSAsyncClient.asyncBuilder().build();
    }

    public Mono<String> encrypt(String original) {
        return Mono.fromCallable(() -> {
            try {
                val bufferedOriginal = StringToByteBuffer(original);
                val encryptRequest = new EncryptRequest();
                encryptRequest.withKeyId(keyId);
                encryptRequest.setPlaintext(bufferedOriginal);

                val result = kms.encrypt(encryptRequest);
                val bufferedCipher = result.getCiphertextBlob();
                return ByteBufferToString(bufferedCipher);

            } catch (UnsupportedEncodingException e) {
                // Shouldn't be reachable, handle anyway
                log.error(e.getMessage(), e);
                return null;
            } catch (AWSKMSException e) {
                log.error("AWSKMSException occurs when encrypting with message {}", e.getMessage());
                return null;
            }
        }).subscribeOn(Schedulers.boundedElastic());

    }

    public Mono<String> decrypt(String cipher) {
        return Mono.fromCallable(() -> {
            try {
                val bufferedCipher = StringToByteBuffer(cipher);
                val decryptRequest = new DecryptRequest();
                decryptRequest.setCiphertextBlob(bufferedCipher);

                val result = kms.decrypt(decryptRequest);
                val bufferedOriginal = result.getPlaintext();
                return ByteBufferToString(bufferedOriginal);

            } catch (UnsupportedEncodingException e) {
                // Shouldn't be reachable, handle anyways
                log.error(e.getMessage(), e);
                return null;
            }
        }).subscribeOn(Schedulers.boundedElastic());

    }

    private ByteBuffer StringToByteBuffer(String string) throws UnsupportedEncodingException {
        val bytes = string.getBytes(StandardCharsets.ISO_8859_1);
        return ByteBuffer.wrap(bytes);
    }

    private String ByteBufferToString(ByteBuffer buffer) throws UnsupportedEncodingException {
        byte[] bytes;
        bytes = buffer.array();
        return new String(bytes, StandardCharsets.ISO_8859_1);
    }
}