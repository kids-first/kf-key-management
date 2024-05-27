package io.kidsfirst.keys;

import io.kidsfirst.core.model.Secret;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import net.minidev.json.JSONObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Slf4j
public class KeyStoreTests extends AbstractTest {

    private final String keyStoreUri = "/key-store";
    protected static String defaultAccessToken = "";
    @BeforeAll
    private static void initTest(){
        val userAndToken = createUserAndSecretAndObtainAccessToken("cavatica", "cavatica_secret");
        defaultAccessToken = userAndToken.getAccessToken();
    }

    @Test
    void testKeyStorePreflight() {
        webClient.options()
                .uri(keyStoreUri)
                .exchange()
                .expectStatus().is2xxSuccessful()
                .expectHeader()

                .value("Allow", matchValues("PUT", "DELETE", "GET", "HEAD", "OPTIONS"));


    }

    @Test
    void testKeyStoreGETContentTypeDifferentThanTextPlain() {
        webClient.get()
                .uri(keyStoreUri)
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + defaultAccessToken)
                .exchange()
                .expectStatus().isEqualTo(406)
                .expectHeader();

    }

    @Test
    void testKeyStoreGET() {
        val body = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(keyStoreUri)
                        .queryParam("service", "cavatica")
                        .build())
                .accept(MediaType.TEXT_PLAIN)
                .header("Authorization", "Bearer " + defaultAccessToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody().returnResult().getResponseBody();
        assert body != null;
        assertThat(new String(body)).isEqualTo("cavatica_secret");
    }

    @Test
    void testKeyStoreDELETE() throws ExecutionException, InterruptedException {
        val userIdAndToken = createUserAndSecretAndObtainAccessToken("cavatica", "my_secret");
        JSONObject body = new JSONObject();
        body.put("service", "cavatica");
        webClient
                .method(HttpMethod.DELETE)
                .uri(keyStoreUri)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + userIdAndToken.getAccessToken())
                .bodyValue(body.toJSONString())
                .exchange()
                .expectStatus().isOk();
        val secret = secretTable.getItem(new Secret(userIdAndToken.getUserId(), "cavatica", null, null)).get();
        assertThat(secret).isNull();
    }

    @Test
    void testKeyStorePUT() throws Exception {
        String my_secret = "my_secret";
        JSONObject body = new JSONObject();
        body.put("service", "cavatica");
        body.put("secret", my_secret);
        val userIdAndToken = createUserAndSecretAndObtainAccessToken("cavatica", my_secret);
        webClient
                .put()
                .uri(keyStoreUri)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + userIdAndToken.getAccessToken())
                .bodyValue(body.toJSONString())
                .exchange()
                .expectStatus().isOk()
                .expectBody();

        val secret = secretTable.getItem(new Secret(userIdAndToken.getUserId(), "cavatica", null, null)).get();
        assertThat(secret).isNotNull();
        assertThat(secret.getSecret()).isEqualTo("encrypted_" + my_secret);
    }

}
