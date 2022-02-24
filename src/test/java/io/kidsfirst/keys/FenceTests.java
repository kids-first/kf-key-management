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

import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Slf4j
public class FenceTests extends AbstractTest {

    private final String fenceAuthClientUri = "/fence/gen3/info";
    private final String fenceAuthenticatedUri = "/fence/gen3/authenticated";
    private final String fenceExchangeUri = "/fence/gen3/exchange";

    protected static String defaultAccessToken = "";
    @BeforeAll
    private static void initTest(){
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
                .json("{\"scope\":\"openid\",\"redirect_uri\":\"https://portal.kidsfirstdrc.org/dcf_redirect/\",\"client_id\":\"dcf_client_id\"}");

    }

    @Test
    void testFenceGEN3AuthClientGET() {
        webClient.get()
                .uri(fenceAuthClientUri)
                .header("Authorization", "Bearer " + defaultAccessToken)

                .exchange()
                .expectStatus().is2xxSuccessful()
                .expectBody().jsonPath("$.scope").exists()
                .json("{\"scope\":\"openid\",\"redirect_uri\":\"https://portal.kidsfirstdrc.org/gen3_redirect/\",\"client_id\":\"gen3_client_id\", \"proxy_uri\": \"/gen3\"}");
    }



    @Test
    void testFenceTokenExchange() throws Exception {
        val userIdAndToken = createUserAndSecretAndObtainAccessToken("other", "this_is_access_token");
        JSONObject content = new JSONObject();
        content.put("access_token", "this_is_access_token");
        content.put("refresh_token", "this_is_refresh_token");
        content.put("token_type", "BEARER");
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
                .expectStatus().isOk();


        val accessSecret = secretTable.getItem(new Secret(userIdAndToken.getUserId(), "fence_gen3_access", null)).get();
        assertThat(accessSecret).isNotNull();
        assertThat(accessSecret.getSecret()).isEqualTo("encrypted_this_is_access_token");

        val refreshSecret = secretTable.getItem(new Secret(userIdAndToken.getUserId(), "fence_gen3_refresh", null)).get();
        assertThat(refreshSecret).isNotNull();
        assertThat(refreshSecret.getSecret()).isEqualTo("encrypted_this_is_refresh_token");

    }

    @Test
    void testFenceAuthenticated() {
        val userIdAndToken = createUserAndSecretAndObtainAccessToken("fence_gen3_access", "this_is_access_token");

        webClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path(fenceAuthenticatedUri)
                        .build())
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + userIdAndToken.getAccessToken())
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.authenticated").value(s->assertThat(s).isEqualTo(true));


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
                .expectBody().jsonPath("$.authenticated").value(s->assertThat(s).isEqualTo(false));

    }

}
