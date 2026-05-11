package io.kidsfirst.keys;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import io.kidsfirst.config.AllFences;
import io.kidsfirst.core.service.FenceService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import reactor.test.StepVerifier;

import java.time.Duration;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

public class FenceServiceTimeoutTest {

    @RegisterExtension
    static WireMockExtension slowFence = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @Test
    void refreshTokensFailsFastWhenFenceStalls() {
        slowFence.stubFor(post(urlPathEqualTo("/oauth/token"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withFixedDelay(60_000)));

        AllFences.Fence fence = new AllFences.Fence();
        fence.setName("slowtest");
        fence.setTokenEndpoint(slowFence.baseUrl() + "/oauth/token");
        fence.setClientId("c");
        fence.setClientSecret("s");

        FenceService service = new FenceService(null);

        // 15s sits between FenceService's 10s read timeout (must fire before this)
        // and WireMock's 60s upstream delay (must trigger after this without the fix).
        StepVerifier.create(service.refreshTokens("any-rt", fence))
                .expectError()
                .verify(Duration.ofSeconds(15));
    }
}
