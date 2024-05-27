package io.kidsfirst.keys;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import io.kidsfirst.core.model.Secret;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import net.minidev.json.JSONObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.time.temporal.ChronoUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static java.time.Instant.now;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Slf4j
public class FenceTests extends AbstractTest {

    private final String fenceAuthClientUri = "/fence/gen3/info";
    private final String fenceAuthenticatedUri = "/fence/gen3/authenticated";
    private final String fenceExchangeUri = "/fence/gen3/exchange";

    protected static String defaultAccessToken = "";

    @BeforeAll
    private static void initTest() {
        val userAndToken = createUserAndSecretAndObtainAccessToken("gen3", "cavatica_secret");
        defaultAccessToken = userAndToken.getAccessToken();
    }

    @RegisterExtension
    static WireMockExtension gen3VM = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @DynamicPropertySource
    static void setGen3Uri(DynamicPropertyRegistry registry) {
        registry.add("application.fence.gen3.token_endpoint",
                () -> gen3VM.baseUrl());
    }


    @Test
    void testFenceAuthClientPreflight() {
        webClient.options()
                .uri(fenceAuthClientUri)
                .exchange()
                .expectStatus().is2xxSuccessful()
                .expectHeader().value("Allow", matchValues("GET", "HEAD", "OPTIONS"));

    }

    @Test
    void testFenceDCFAuthClientGET() {
        String dcfAuthClientUri = "/fence/dcf/info";
        webClient.get()
                .uri(dcfAuthClientUri)
                .header("Authorization", "Bearer " + defaultAccessToken)
                .exchange()
                .expectStatus().is2xxSuccessful()
                .expectBody().jsonPath("$.scope").exists()
                .json("{\"scope\":\"openid\",\"redirect_uri\":\"https://portal.kidsfirstdrc.org/dcf_redirect/\",\"client_id\":\"dcf_client_id\"}")
                .jsonPath("$.authorize_uri").isEqualTo("https://nci-crdc-staging.datacommons.io/user/oauth2/authorize?idp=ras&scope=openid&client_id=dcf_client_id&redirect_uri=https://portal.kidsfirstdrc.org/dcf_redirect/&response_type=code")
        ;

    }

    @Test
    void testFenceGEN3AuthClientGET() {
        webClient.get()
                .uri(fenceAuthClientUri)
                .header("Authorization", "Bearer " + defaultAccessToken)

                .exchange()
                .expectStatus().is2xxSuccessful()
                .expectBody().jsonPath("$.scope").exists()
                .json("{\"scope\":\"openid%20user\",\"redirect_uri\":\"https://portal.kidsfirstdrc.org/gen3_redirect/\",\"client_id\":\"gen3_client_id\", \"proxy_uri\": \"/gen3\"}")
                .jsonPath("$.authorize_uri").isEqualTo("https://gen3staging.kidsfirstdrc.org/user/oauth2/authorize?idp=ras&scope=openid%20user&client_id=gen3_client_id&redirect_uri=https://portal.kidsfirstdrc.org/gen3_redirect/&response_type=code");
    }


    @Test
    void testFenceTokenExchange() throws Exception {
        val expiration = now().minus(10, ChronoUnit.SECONDS).getEpochSecond();
        val userIdAndToken = createUserAndSecretAndObtainAccessToken("fence_gen3_access", "this_is_access_token", expiration);
        createUserAndSecretAndObtainAccessToken("fence_gen3_refresh", "this_is_refresh_token", expiration);
        JSONObject content = new JSONObject();
        content.put("access_token", "this_is_fresh_access_token");
        content.put("refresh_token", "this_is_fresh_refresh_token");
        content.put("token_type", "BEARER");
        content.put("expires_in", 1200);
        gen3VM.stubFor(post("/").willReturn(ok(content.toJSONString()).withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)));
        webClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path(fenceExchangeUri)
                        .queryParam("code", "anAuthCodeValue")
                        .build())
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + userIdAndToken.getAccessToken())
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.expiration").exists()
        ;


        val accessSecret = secretTable.getItem(new Secret(userIdAndToken.getUserId(), "fence_gen3_access", null, null)).get();
        assertThat(accessSecret).isNotNull();
        assertThat(accessSecret.getSecret()).isEqualTo("encrypted_this_is_fresh_access_token");
        assertThat(accessSecret.notExpired()).isTrue();
        assertThat(accessSecret.getExpiration()).isGreaterThan(expiration);

        val refreshSecret = secretTable.getItem(new Secret(userIdAndToken.getUserId(), "fence_gen3_refresh", null, null)).get();
        assertThat(refreshSecret).isNotNull();
        assertThat(refreshSecret.getSecret()).isEqualTo("encrypted_this_is_fresh_refresh_token");
        assertThat(refreshSecret.notExpired()).isTrue();
        assertThat(refreshSecret.getExpiration()).isGreaterThan(expiration);

    }

    @Test
    void testFenceAuthenticatedWithRefreshTokenValidOnly() {
        val expiration = now().plus(10, ChronoUnit.SECONDS).getEpochSecond();
        val userIdAndToken = createUserAndSecretAndObtainAccessToken("fence_gen3_refresh", "this_is_refresh_token", expiration);

        webClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path(fenceAuthenticatedUri)
                        .build())
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + userIdAndToken.getAccessToken())
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.authenticated").isEqualTo(true)
                .jsonPath("$.expiration").isEqualTo(expiration);

    }

    @Test
    void testFenceAuthenticatedWithRefreshTokenExpiredOnly() {
        val expiration = now().minus(10, ChronoUnit.SECONDS).getEpochSecond();
        val userIdAndToken = createUserAndSecretAndObtainAccessToken("fence_gen3_refresh", "this_is_refresh_token", expiration);

        webClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path(fenceAuthenticatedUri)
                        .build())
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + userIdAndToken.getAccessToken())
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.authenticated").isEqualTo(false);

    }

    @Test
    void testFenceAuthenticatedWithAccessTokenValidOnly() {
        val expiration = now().plus(10, ChronoUnit.SECONDS).getEpochSecond();
        val userIdAndToken = createUserAndSecretAndObtainAccessToken("fence_gen3_access", "this_is_access_token", expiration);

        webClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path(fenceAuthenticatedUri)
                        .build())
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + userIdAndToken.getAccessToken())
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.authenticated").isEqualTo(true)
                .jsonPath("$.expiration").isEqualTo(expiration);

    }


    @Test
    void testFenceAuthenticatedWithAccessTokenExpiredOnly() {
        val expiration = now().minus(10, ChronoUnit.SECONDS).getEpochSecond();
        val userIdAndToken = createUserAndSecretAndObtainAccessToken("fence_gen3_access", "this_is_access_token", expiration);

        webClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path(fenceAuthenticatedUri)
                        .build())
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + userIdAndToken.getAccessToken())
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.authenticated").isEqualTo(false);

    }

    @Test
    void testFenceAuthenticatedWithBothTokenExpired() {
        val expiration = now().minus(10, ChronoUnit.SECONDS).getEpochSecond();
        val userIdAndToken = createUserAndSecretAndObtainAccessToken("fence_gen3_access", "this_is_access_token", expiration);
        createSecret("fence_gen3_access", userIdAndToken.getUserId(), "this_is_access_token", expiration);

        webClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path(fenceAuthenticatedUri)
                        .build())
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + userIdAndToken.getAccessToken())
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.authenticated").isEqualTo(false);

    }

    @Test
    void testFenceAuthenticatedWithBothTokenButAccessExpired() {
        val expirationRefresh = now().plus(10, ChronoUnit.SECONDS).getEpochSecond();
        val userIdAndToken = createUserAndSecretAndObtainAccessToken("fence_gen3_refresh", "this_is_refresh_token", expirationRefresh);
        val expirationAccess = now().minus(10, ChronoUnit.SECONDS).getEpochSecond();
        createSecret("fence_gen3_access", userIdAndToken.getUserId(), "this_is_access_token", expirationAccess);

        webClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path(fenceAuthenticatedUri)
                        .build())
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + userIdAndToken.getAccessToken())
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.authenticated").isEqualTo(true)
                .jsonPath("$.expiration").isEqualTo(expirationRefresh);
    }

    @Test
    void testFenceAuthenticatedWithBothTokenButRefreshExpired() {
        val expirationRefresh = now().minus(10, ChronoUnit.SECONDS).getEpochSecond();
        val userIdAndToken = createUserAndSecretAndObtainAccessToken("fence_gen3_refresh", "this_is_refresh_token", expirationRefresh);
        val expirationAccess = now().plus(10, ChronoUnit.SECONDS).getEpochSecond();
        createSecret("fence_gen3_access", userIdAndToken.getUserId(), "this_is_access_token", expirationAccess);
        webClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path(fenceAuthenticatedUri)
                        .build())
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + userIdAndToken.getAccessToken())
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.authenticated").isEqualTo(true)
                .jsonPath("$.expiration").isEqualTo(expirationAccess);
    }

    @Test
    void testFenceAuthenticatedWithBothTokenButRefreshExpireAfterAccess() {
        val expirationRefresh = now().plus(30, ChronoUnit.SECONDS).getEpochSecond();
        val userIdAndToken = createUserAndSecretAndObtainAccessToken("fence_gen3_refresh", "this_is_refresh_token", expirationRefresh);
        val expirationAccess = now().plus(10, ChronoUnit.SECONDS).getEpochSecond();
        createSecret("fence_gen3_access", userIdAndToken.getUserId(), "this_is_access_token", expirationAccess);
        webClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path(fenceAuthenticatedUri)
                        .build())
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + userIdAndToken.getAccessToken())
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.authenticated").isEqualTo(true)
                .jsonPath("$.expiration").isEqualTo(expirationRefresh);
    }

    @Test
    void testFenceAuthenticatedWithBothTokenButAccessExpireAfterRefresh() {
        val expirationRefresh = now().plus(10, ChronoUnit.SECONDS).getEpochSecond();
        val userIdAndToken = createUserAndSecretAndObtainAccessToken("fence_gen3_refresh", "this_is_refresh_token", expirationRefresh);
        val expirationAccess = now().plus(30, ChronoUnit.SECONDS).getEpochSecond();
        createSecret("fence_gen3_access", userIdAndToken.getUserId(), "this_is_access_token", expirationAccess);
        webClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path(fenceAuthenticatedUri)
                        .build())
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + userIdAndToken.getAccessToken())
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.authenticated").isEqualTo(true)
                .jsonPath("$.expiration").isEqualTo(expirationAccess);
    }

    @Test
    void testFenceAuthenticatedNotConnected() {
        val userIdAndToken = createUserAndSecretAndObtainAccessToken("other", "this_is_access_token");

        webClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path(fenceAuthenticatedUri)
                        .build())
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + userIdAndToken.getAccessToken())
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.authenticated").value(s -> assertThat(s).isEqualTo(false));

    }

}
