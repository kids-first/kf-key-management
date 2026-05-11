package io.kidsfirst.keys;

import org.junit.jupiter.api.Test;

public class SecurityRulesTests extends AbstractTest {

    // Rule: pathMatchers("/status").permitAll()
    @Test
    void statusIsAnonymouslyAccessible() {
        webClient.get().uri("/status")
                .exchange()
                .expectStatus().isOk();
    }

    // Rule: pathMatchers("/auth-client").permitAll()
    @Test
    void authClientPermitRuleIsHonoredEvenThoughEndpointIsAbsent() {
        webClient.get().uri("/auth-client")
                .exchange()
                .expectStatus().isNotFound();
    }

    // Rule: anyExchange().authenticated()
    @Test
    void normalEndpointRequiresAuthentication() {
        webClient.get().uri("/fence/gen3/authenticated")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    // Rule: anyExchange().authenticated() — pins current /info behavior
    // until polish-plan Tier 2 #5 resolves the doc/code mismatch.
    @Test
    void fenceInfoCurrentlyRequiresAuthenticationDespiteDocs() {
        webClient.get().uri("/fence/gen3/info")
                .exchange()
                .expectStatus().isUnauthorized();
    }
}
