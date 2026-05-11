package io.kidsfirst.core.service;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.kms.KmsAsyncClient;
import software.amazon.awssdk.services.kms.model.CreateKeyRequest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import reactor.test.StepVerifier;

import static org.testcontainers.containers.localstack.LocalStackContainer.Service.KMS;

@Testcontainers
class AwsKmsServiceTest {

    @Container
    static final LocalStackContainer LOCALSTACK = new LocalStackContainer(
            DockerImageName.parse("localstack/localstack:3.8"))
            .withServices(KMS);

    static AwsKmsService kmsService;

    @BeforeAll
    static void setUp() {
        KmsAsyncClient kmsClient = KmsAsyncClient.builder()
                .endpointOverride(LOCALSTACK.getEndpointOverride(KMS))
                .region(Region.of(LOCALSTACK.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(LOCALSTACK.getAccessKey(), LOCALSTACK.getSecretKey())))
                .build();
        String keyId = kmsClient.createKey(CreateKeyRequest.builder().build())
                .join().keyMetadata().keyId();
        kmsService = new AwsKmsService(keyId, kmsClient);
    }

    @Test
    void testEncryptDecryptRoundTrip() {
        String plaintext = "hello-kms";
        StepVerifier.create(kmsService.encrypt(plaintext).flatMap(kmsService::decrypt))
                .expectNext(plaintext)
                .verifyComplete();
    }

    @Test
    void testCompressEncryptDecryptDecompressRoundTrip() {
        String plaintext = "hello-kms-gzip-path-with-some-padding-to-be-realistic";
        StepVerifier.create(kmsService.compressAndEncrypt(plaintext).flatMap(kmsService::decryptAndDecompress))
                .expectNext(plaintext)
                .verifyComplete();
    }
}
