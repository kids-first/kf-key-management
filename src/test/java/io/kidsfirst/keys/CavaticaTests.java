package io.kidsfirst.keys;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import net.minidev.json.JSONObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

@Slf4j
public class CavaticaTests extends AbstractTest {

    protected static String defaultAccessToken = "";
    private final String cavaticaURI = "/cavatica";
    private final String cavaticaResponseBody = "{" +
            "  \"href\": \"https://cavatica-api.sbgenomics.com/v2/users/RFranklin\"," +
            "  \"username\": \"RFranklin\"," +
            "  \"email\": \"rosalind.franklin@sbgenomics.com\"," +
            "  \"first_name\": \"Rosalind\"," +
            "  \"last_name\": \"Franklin\"," +
            "  \"tags\": [" +
            "        {" +
            "            \"tag\": \"tcga-oa\"," +
            "            \"expires_at\": 3053937952000" +
            "        }," +
            "        {" +
            "            \"tag\": \"tcga-ca\"," +
            "            \"expires_at\": 1506729600000" +
            "        }" +
            "   ]," +
            "  \"affiliation\": \"Seven Bridges\"," +
            "  \"phone\": \"\"," +
            "  \"address\": \"\"," +
            "  \"city\": \"London\"," +
            "  \"state\": \"\"," +
            "  \"country\": \"United Kingdom\"," +
            "  \"zip_code\": \"\"" +
            "}";

    @BeforeAll
    private static void initTest(){
        val userAndToken = createUserAndSecretAndObtainAccessToken("cavatica", "cavatica_secret");
        defaultAccessToken = userAndToken.getAccessToken();
    }

    @RegisterExtension
    static WireMockExtension cavaticaWM = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @DynamicPropertySource
    static void setCavaticaUri(DynamicPropertyRegistry registry) {
        registry.add("application.cavatica_root",
                () -> cavaticaWM.baseUrl());
    }

    @Test
    public void testCavaticaPreflight() {
        webClient.options()
                .uri(cavaticaURI)
                .exchange()
                .expectStatus().is2xxSuccessful()
                .expectHeader().value("Allow", matchValues("POST", "OPTIONS"));
    }

    @Test
    void testCavaticaPostWithoutToken() {
        JSONObject content = new JSONObject();
        content.put("path", "/user");
        content.put("method", "GET");

        JSONObject body = new JSONObject();
        body.put("key1", "value1");
        body.put("key2", "value2");
        content.put("body", body);

        webClient.post()
                .uri(cavaticaURI)
                .bodyValue(content.toJSONString())
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void testHealthStatus() {
        webClient.get().uri("/status")
                .exchange()
                .expectStatus().isOk()
                .expectBody().json("{\"status\": \"OK\"}");
    }

    @Test
    void testCavaticaPostWithoutBody() {
        JSONObject content = new JSONObject();
        content.put("path", "/user");
        content.put("method", "GET");
        cavaticaWM.stubFor(get("/user").willReturn(ok(cavaticaResponseBody)));
        webClient.post()
                .uri("/cavatica")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + defaultAccessToken)
                .bodyValue(content.toJSONString())
                .exchange()
                .expectStatus().isOk()
                .expectBody().json(cavaticaResponseBody);

    }


    @Test
    void testCavaticaPostWithBody() {
        JSONObject content = new JSONObject();
        content.put("path", "/user");
        content.put("method", "GET");

        JSONObject body = new JSONObject();
        body.put("key1", "value1");
        body.put("key2", "value2");
        content.put("body", body);

        cavaticaWM.stubFor(get("/user").willReturn(ok(cavaticaResponseBody)));
        webClient.post()
                .uri("/cavatica")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + defaultAccessToken)
                .bodyValue(content.toJSONString())
                .exchange()
                .expectStatus().isOk()
                .expectBody().json(cavaticaResponseBody);
    }

    @Test
    void testCavaticaUnsupported() {
        JSONObject content = new JSONObject();
        content.put("path", "/user");
        content.put("method", "UNSUPORTED");
        cavaticaWM.stubFor(get("/user").willReturn(ok(cavaticaResponseBody)));
        webClient.post()
                .uri("/cavatica")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + defaultAccessToken)
                .bodyValue(content.toJSONString())
                .exchange()
                .expectStatus().isEqualTo(400);
    }


}
