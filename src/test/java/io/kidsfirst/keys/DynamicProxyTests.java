package io.kidsfirst.keys;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.nimbusds.jose.shaded.json.JSONObject;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
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
public class DynamicProxyTests extends AbstractTest {

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
        registry.add("application.fence.gen3.api_endpoint",
                () -> gen3VM.baseUrl());
    }

    @Test
    void testProxy()  {
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
                .expectStatus().isOk()
                .expectBody().json(content.toJSONString());

    }

    @Test
    void testProxyWithoutAcessToken()  {
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
