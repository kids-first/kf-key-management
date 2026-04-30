package io.kidsfirst.keys;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import lombok.val;
import net.minidev.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.time.temporal.ChronoUnit;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static java.time.Instant.now;

/**
 * Pins the concurrent-refresh fan-out behavior in {@link io.kidsfirst.web.rest.FenceAuthFilterFactory}:
 * N parallel proxy requests for the same (user, fence) tuple whose access
 * token has expired each independently trigger their own POST to the fence's
 * token endpoint, instead of sharing a single in-flight refresh.
 *
 * <p>Combined with refresh-token rotation (RFC 6749 §10.4 permits, and
 * RFC 9700 — OAuth 2.0 Security Best Current Practice — effectively requires
 * rotation for public clients; Gen3 and DCF do rotate), this can leave the
 * stored refresh token desynchronized from the fence's view, manifesting as
 * silent re-auth requirements for the user.
 *
 * <p>This test asserts the <b>buggy current behavior</b> ({@code exactly(N)}),
 * not the desired one. The single-flight fix is deferred to a separate PR.
 * When that fix lands, this test will fail and the assertion should flip
 * to {@code exactly(1)}.
 */
public class FenceConcurrentRefreshFanOutTest extends AbstractTest {

    private static final int CONCURRENT_REQUESTS = 5;

    @RegisterExtension
    static WireMockExtension fenceVM = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @DynamicPropertySource
    static void setFenceUri(DynamicPropertyRegistry registry) {
        registry.add("application.fence.gen3.token_endpoint", fenceVM::baseUrl);
        registry.add("application.fence.gen3.api_endpoint", fenceVM::baseUrl);
    }

    @Test
    void concurrentRequestsFanOutToTheFence() {
        val expirationAccess = now().minus(10, ChronoUnit.SECONDS).getEpochSecond();
        val userAndToken = createUserAndSecretAndObtainAccessToken(
                "fence_gen3_access", "expired_at", expirationAccess);
        val expirationRefresh = now().plus(10, ChronoUnit.SECONDS).getEpochSecond();
        createSecret("fence_gen3_refresh", userAndToken.getUserId(), "valid_rt", expirationRefresh);

        JSONObject refreshContent = new JSONObject();
        refreshContent.put("access_token", "fresh_at");
        refreshContent.put("refresh_token", "fresh_rt");
        refreshContent.put("token_type", "BEARER");
        refreshContent.put("expires_in", 1200);
        fenceVM.stubFor(post("/").willReturn(
                ok(refreshContent.toJSONString())
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withFixedDelay(1_000)));

        JSONObject apiContent = new JSONObject();
        apiContent.put("user_id", "119");
        fenceVM.stubFor(get("/user/user").willReturn(
                ok(apiContent.toJSONString())
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)));

        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_REQUESTS);
        try {
            String jwt = userAndToken.getAccessToken();
            CompletableFuture<?>[] futures = IntStream.range(0, CONCURRENT_REQUESTS)
                    .mapToObj(i -> CompletableFuture.runAsync(() ->
                            webClient.get()
                                    .uri("/gen3/user/user")
                                    .header("Authorization", "Bearer " + jwt)
                                    .exchange()
                                    .expectStatus().isOk(), executor))
                    .toArray(CompletableFuture[]::new);
            CompletableFuture.allOf(futures).join();
        } finally {
            executor.shutdown();
        }

        // Asserts the buggy current behavior. When single-flight refresh lands,
        // this should flip to exactly(1).
        fenceVM.verify(exactly(CONCURRENT_REQUESTS), postRequestedFor(urlEqualTo("/")));
    }
}
