package io.kidsfirst.keys;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.nimbusds.jose.shaded.json.JSONObject;
import io.kidsfirst.core.model.Secret;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.time.temporal.ChronoUnit;
import java.util.concurrent.ExecutionException;

import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static java.time.Instant.now;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Slf4j
public class FenceDeprecatedTests extends AbstractTest {


    private final String fenceAuthClientUri = "/auth-client";
    private final String fenceTokenUri = "/token";
    private final String fenceRefreshUri = "/refresh";

    protected static String defaultAccessToken = "";
    @BeforeAll
    private static void initTest(){
        val userAndToken = createUserAndSecretAndObtainAccessToken("cavatica", "cavatica_secret");
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
        webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(fenceAuthClientUri)
                        .queryParam("fence", "dcf")
                        .build())
                .header("Authorization", "Bearer " + defaultAccessToken)

                .exchange()
                .expectStatus().is2xxSuccessful()
                .expectBody().jsonPath("$.scope").exists()
                .json("{\"scope\":\"openid\",\"redirect_uri\":\"https://portal.kidsfirstdrc.org/dcf_redirect/\",\"client_id\":\"dcf_client_id\"}");

    }

    @Test
    void testFenceGEN3AuthClientGET() {
        webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(fenceAuthClientUri)
                        .queryParam("fence", "gen3")
                        .build())
                .header("Authorization", "Bearer " + defaultAccessToken)

                .exchange()
                .expectStatus().is2xxSuccessful()
                .expectBody().jsonPath("$.scope").exists()
                .json("{\"scope\":\"openid\",\"redirect_uri\":\"https://portal.kidsfirstdrc.org/gen3_redirect/\",\"client_id\":\"gen3_client_id\"}");
    }


    @Test
    void testFenceRefreshPreflight() {
        webClient.options()
                .uri(fenceRefreshUri)
                .exchange()
                .expectStatus().is2xxSuccessful()
                .expectHeader()
                .value("Allow", matchValues("POST", "OPTIONS"));

    }

    @Test
    void testFenceRefreshPOST() throws Exception {
        val expirationRefresh = now().plus(10, ChronoUnit.SECONDS).getEpochSecond();
        val userIdAndToken = createUserAndSecretAndObtainAccessToken("fence_gen3_refresh", "secret", expirationRefresh);
        JSONObject content = new JSONObject();
        content.put("access_token", "this_is_access_token");
        content.put("refresh_token", "this_is_a_fresh_refresh_token");
        content.put("token_type", "BEARER");
        gen3VM.stubFor(post("/").willReturn(ok(content.toJSONString()).withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)));
        webClient
                .post()
                .uri(uriBuilder -> uriBuilder
                        .path(fenceRefreshUri)
                        .queryParam("fence", "gen3")
                        .build())
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + userIdAndToken.getAccessToken())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.access_token").value(o -> assertThat(o).isEqualTo("this_is_access_token"))
                .jsonPath("$.refresh_token").value(o -> assertThat(o).isEqualTo("this_is_a_fresh_refresh_token"));

        val accessSecret = secretTable.getItem(new Secret(userIdAndToken.getUserId(), "fence_gen3_access", null, null)).get();
        assertThat(accessSecret).isNotNull();
        assertThat(accessSecret.getSecret()).isEqualTo("encrypted_this_is_access_token");

        val refreshSecret = secretTable.getItem(new Secret(userIdAndToken.getUserId(), "fence_gen3_refresh", null, null)).get();
        assertThat(refreshSecret).isNotNull();
        assertThat(refreshSecret.getSecret()).isEqualTo("encrypted_this_is_a_fresh_refresh_token");
        assertThat(refreshSecret.getExpiration()).isEqualTo(expirationRefresh);

    }


    @Test
    void testFenceTokenPreflight() {
        webClient.options()
                .uri(fenceTokenUri)
                .exchange()
                .expectStatus().is2xxSuccessful()
                .expectHeader()

                .value("Allow", matchValues("GET", "HEAD", "DELETE", "POST", "OPTIONS"));

    }

    @Test
    void testFenceTokenDELETE() throws ExecutionException, InterruptedException {
        val userIdAndToken = createUserAndSecretAndObtainAccessToken("fence_gen3_access", "secret");
        createSecret("fence_gen3_refresh", userIdAndToken.getUserId(), "secret");
        webClient
                .delete()
                .uri(uriBuilder -> uriBuilder
                        .path(fenceTokenUri)
                        .queryParam("fence", "gen3")
                        .build())
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + userIdAndToken.getAccessToken())
                .exchange().expectStatus().isOk();
        val accessSecret = secretTable.getItem(new Secret(userIdAndToken.getUserId(), "fence_gen3_access", null, null)).get();
        assertThat(accessSecret).isNull();

        val refreshSecret = secretTable.getItem(new Secret(userIdAndToken.getUserId(), "fence_gen3_refresh", null, null)).get();
        assertThat(refreshSecret).isNull();


    }



    @Test
    void testFenceGEN3TokenGET() {
        val userIdAndToken = createUserAndSecretAndObtainAccessToken("fence_gen3_access", "this_is_access_token");
        createSecret("fence_gen3_refresh", userIdAndToken.getUserId(), "this_is_refresh_token");
        webClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path(fenceTokenUri)
                        .queryParam("fence", "gen3")
                        .build())
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + userIdAndToken.getAccessToken())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.access_token").value(o -> assertThat(o).isEqualTo("this_is_access_token"));

    }

    @Test
    void testFenceTokenPOST() throws Exception {
        val userIdAndToken = createUserAndSecretAndObtainAccessToken("other", "this_is_access_token");
        JSONObject content = new JSONObject();
        content.put("access_token", "this_is_access_token");
        content.put("refresh_token", "this_is_refresh_token");
        content.put("token_type", "BEARER");
        gen3VM.stubFor(post("/").willReturn(ok(content.toJSONString()).withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)));
        webClient
                .post()
                .uri(uriBuilder -> uriBuilder
                        .path(fenceTokenUri)
                        .queryParam("fence", "gen3")
                        .queryParam("code", "anAuthCodeValue")
                        .build())
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + userIdAndToken.getAccessToken())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.access_token").value(o -> assertThat(o).isEqualTo("this_is_access_token"))
                .jsonPath("$.refresh_token").value(o -> assertThat(o).isEqualTo("this_is_refresh_token"));

        val accessSecret = secretTable.getItem(new Secret(userIdAndToken.getUserId(), "fence_gen3_access", null, null)).get();
        assertThat(accessSecret).isNotNull();
        assertThat(accessSecret.getSecret()).isEqualTo("encrypted_this_is_access_token");

        val refreshSecret = secretTable.getItem(new Secret(userIdAndToken.getUserId(), "fence_gen3_refresh", null, null)).get();
        assertThat(refreshSecret).isNotNull();
        assertThat(refreshSecret.getSecret()).isEqualTo("encrypted_this_is_refresh_token");

    }

}
