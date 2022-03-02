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

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static java.time.Instant.now;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Slf4j
public class DynamicProxyTests extends AbstractTest {

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
        registry.add("application.fence.gen3.api_endpoint",
                () -> gen3VM.baseUrl());
    }

    @Test
    void testProxyWithAccessTokenValid() {
        val expiration = now().plus(10, ChronoUnit.SECONDS).getEpochSecond();
        val userIdAndToken = createUserAndSecretAndObtainAccessToken("fence_gen3_access", "this_is_access_token", expiration);
        JSONObject content = new JSONObject();
        content.put("user_id", "119");
        content.put("username", "DoeJ");
        gen3VM.stubFor(get("/user/user").willReturn(ok(content.toJSONString()).withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)));
        webClient
                .get()
                .uri("/gen3/user/user")
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + userIdAndToken.getAccessToken())
                .exchange()
                .expectStatus().isOk()
                .expectBody().json(content.toJSONString());

    }


    @Test
    void testProxyWithAccessTokenWithoutExpiration() {
        val userIdAndToken = createUserAndSecretAndObtainAccessToken("fence_gen3_access", "this_is_access_token");
        JSONObject content = new JSONObject();
        content.put("user_id", "119");
        content.put("username", "DoeJ");
        gen3VM.stubFor(get("/user/user").willReturn(ok(content.toJSONString()).withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)));
        webClient
                .get()
                .uri("/gen3/user/user")
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + userIdAndToken.getAccessToken())
                .exchange()
                .expectStatus().isUnauthorized();

    }

    @Test
    void testProxyWithAccessTokenExpired() {
        val expiration = now().minus(10, ChronoUnit.SECONDS).getEpochSecond();
        val userIdAndToken = createUserAndSecretAndObtainAccessToken("fence_gen3_access", "this_is_access_token", expiration);
        JSONObject content = new JSONObject();
        content.put("user_id", "119");
        content.put("username", "DoeJ");
        gen3VM.stubFor(get("/user/user").willReturn(ok(content.toJSONString()).withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)));
        webClient
                .get()
                .uri("/gen3/user/user")
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + userIdAndToken.getAccessToken())
                .exchange()
                .expectStatus().isUnauthorized();

    }

    @Test
    void testProxyWithBothTokenExpired() {
        val expiration = now().minus(10, ChronoUnit.SECONDS).getEpochSecond();
        val userIdAndToken = createUserAndSecretAndObtainAccessToken("fence_gen3_access", "this_is_access_token", expiration);
        createSecret("fence_gen3_refresh", userIdAndToken.getUserId(), "this_is_refresh_token", expiration);
        JSONObject content = new JSONObject();
        content.put("user_id", "119");
        content.put("username", "DoeJ");
        gen3VM.stubFor(get("/user/user").willReturn(ok(content.toJSONString()).withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)));
        webClient
                .get()
                .uri("/gen3/user/user")
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + userIdAndToken.getAccessToken())
                .exchange()
                .expectStatus().isUnauthorized();

    }
    @Test
    void testProxyWithBothTokenWithoutExpiration() {
        val userIdAndToken = createUserAndSecretAndObtainAccessToken("fence_gen3_access", "this_is_access_token");
        createSecret("fence_gen3_refresh", userIdAndToken.getUserId(), "this_is_refresh_token");
        JSONObject content = new JSONObject();
        content.put("user_id", "119");
        content.put("username", "DoeJ");
        gen3VM.stubFor(get("/user/user").willReturn(ok(content.toJSONString()).withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)));
        webClient
                .get()
                .uri("/gen3/user/user")
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + userIdAndToken.getAccessToken())
                .exchange()
                .expectStatus().isUnauthorized();

    }

    @Test
    void testProxyWithBothTokenAndRefreshValid() throws ExecutionException, InterruptedException {
        val expirationAccess = now().minus(10, ChronoUnit.SECONDS).getEpochSecond();
        val userIdAndToken = createUserAndSecretAndObtainAccessToken("fence_gen3_access", "this_is_an_expired_access_token", expirationAccess);
        val expirationRefresh = now().plus(10, ChronoUnit.SECONDS).getEpochSecond();
        createSecret("fence_gen3_refresh", userIdAndToken.getUserId(), "this_is_refresh_token", expirationRefresh);

        JSONObject refreshContent = new JSONObject();
        refreshContent.put("access_token", "this_is_a_fresh_access_token");
        refreshContent.put("refresh_token", "this_is_a_fresh_refresh_token");
        refreshContent.put("token_type", "BEARER");
        refreshContent.put("expires_in", 1200);
        gen3VM.stubFor(post("/").willReturn(ok(refreshContent.toJSONString()).withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)));

        JSONObject content = new JSONObject();
        content.put("user_id", "119");
        content.put("username", "DoeJ");
        gen3VM.stubFor(get("/user/user").willReturn(ok(content.toJSONString()).withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)));

        webClient
                .get()
                .uri("/gen3/user/user")
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + userIdAndToken.getAccessToken())
                .exchange()
                .expectStatus().isOk()
                .expectBody().json(content.toJSONString());

        //Verify than access token has been refreshed
        val accessSecret = secretTable.getItem(new Secret(userIdAndToken.getUserId(), "fence_gen3_access", null, null)).get();
        assertThat(accessSecret).isNotNull();
        assertThat(accessSecret.getSecret()).isEqualTo("encrypted_this_is_a_fresh_access_token");
        assertThat(accessSecret.notExpired()).isTrue();

        //Verify than refresh token has been refreshed, except for expiration date
        val refreshSecret = secretTable.getItem(new Secret(userIdAndToken.getUserId(), "fence_gen3_refresh", null, null)).get();
        assertThat(refreshSecret).isNotNull();
        assertThat(refreshSecret.getSecret()).isEqualTo("encrypted_this_is_a_fresh_refresh_token");
        assertThat(refreshSecret.getExpiration()).isEqualTo(expirationRefresh);

    }

    @Test
    void testProxyWithOnlyRefreshTokenValid() throws ExecutionException, InterruptedException {
        val expirationRefresh = now().plus(10, ChronoUnit.SECONDS).getEpochSecond();
        val userIdAndToken = createUserAndSecretAndObtainAccessToken("fence_gen3_refresh", "this_is_refresh_token", expirationRefresh);

        JSONObject refreshContent = new JSONObject();
        refreshContent.put("access_token", "this_is_a_fresh_access_token");
        refreshContent.put("refresh_token", "this_is_a_fresh_refresh_token");
        refreshContent.put("token_type", "BEARER");
        refreshContent.put("expires_in", 1200);
        gen3VM.stubFor(post("/").willReturn(ok(refreshContent.toJSONString()).withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)));

        JSONObject content = new JSONObject();
        content.put("user_id", "119");
        content.put("username", "DoeJ");
        gen3VM.stubFor(get("/user/user").willReturn(ok(content.toJSONString()).withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)));

        webClient
                .get()
                .uri("/gen3/user/user")
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + userIdAndToken.getAccessToken())
                .exchange()
                .expectStatus().isOk()
                .expectBody().json(content.toJSONString());

        //Verify than access token has been refreshed
        val accessSecret = secretTable.getItem(new Secret(userIdAndToken.getUserId(), "fence_gen3_access", null, null)).get();
        assertThat(accessSecret).isNotNull();
        assertThat(accessSecret.getSecret()).isEqualTo("encrypted_this_is_a_fresh_access_token");
        assertThat(accessSecret.notExpired()).isTrue();

        //Verify than refresh token has been refreshed, except for expiration date
        val refreshSecret = secretTable.getItem(new Secret(userIdAndToken.getUserId(), "fence_gen3_refresh", null, null)).get();
        assertThat(refreshSecret).isNotNull();
        assertThat(refreshSecret.getSecret()).isEqualTo("encrypted_this_is_a_fresh_refresh_token");
        assertThat(refreshSecret.getExpiration()).isEqualTo(expirationRefresh);
    }

    @Test
    void testProxyWithRefreshTokenValidAndAccessWithoutExpiration() throws ExecutionException, InterruptedException {
        val userIdAndToken = createUserAndSecretAndObtainAccessToken("fence_gen3_access", "this_is_an_expired_access_token");
        val expirationRefresh = now().plus(10, ChronoUnit.SECONDS).getEpochSecond();
        createSecret("fence_gen3_refresh", userIdAndToken.getUserId(), "this_is_refresh_token", expirationRefresh);

        JSONObject refreshContent = new JSONObject();
        refreshContent.put("access_token", "this_is_a_fresh_access_token");
        refreshContent.put("refresh_token", "this_is_a_fresh_refresh_token");
        refreshContent.put("token_type", "BEARER");
        refreshContent.put("expires_in", 1200);
        gen3VM.stubFor(post("/").willReturn(ok(refreshContent.toJSONString()).withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)));

        JSONObject content = new JSONObject();
        content.put("user_id", "119");
        content.put("username", "DoeJ");
        gen3VM.stubFor(get("/user/user").willReturn(ok(content.toJSONString()).withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)));

        webClient
                .get()
                .uri("/gen3/user/user")
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + userIdAndToken.getAccessToken())
                .exchange()
                .expectStatus().isOk()
                .expectBody().json(content.toJSONString());

        //Verify than access token has been refreshed
        val accessSecret = secretTable.getItem(new Secret(userIdAndToken.getUserId(), "fence_gen3_access", null, null)).get();
        assertThat(accessSecret).isNotNull();
        assertThat(accessSecret.getSecret()).isEqualTo("encrypted_this_is_a_fresh_access_token");
        assertThat(accessSecret.notExpired()).isTrue();

        //Verify than refresh token has been refreshed, except for expiration date
        val refreshSecret = secretTable.getItem(new Secret(userIdAndToken.getUserId(), "fence_gen3_refresh", null, null)).get();
        assertThat(refreshSecret).isNotNull();
        assertThat(refreshSecret.getSecret()).isEqualTo("encrypted_this_is_a_fresh_refresh_token");
        assertThat(refreshSecret.getExpiration()).isEqualTo(expirationRefresh);
    }

    @Test
    void testProxyWithoutAccessToken() {
        val userIdAndToken = createUserAndSecretAndObtainAccessToken("OTHER", "this_is_access_token");
        JSONObject content = new JSONObject();
        content.put("user_id", "119");
        content.put("username", "DoeJ");
        gen3VM.stubFor(get("/user/user").willReturn(ok(content.toJSONString()).withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)));
        webClient
                .get()
                .uri("/gen3/user/user")
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + userIdAndToken.getAccessToken())
                .exchange()
                .expectStatus().isUnauthorized();

    }

}
