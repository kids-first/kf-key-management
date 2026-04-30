package io.kidsfirst.keys;

import org.junit.jupiter.api.Test;

public class HealthTests extends AbstractTest {

    @Test
    void testStatusIsAnonymouslyAccessible() {
        webClient.get().uri("/status")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("OK");
    }
}
