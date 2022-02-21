package io.kidsfirst.keys;

import com.nimbusds.jose.shaded.json.JSONObject;
import io.kidsfirst.core.model.Secret;
import io.kidsfirst.core.service.KMSService;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.CreatedResponseUtil;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.testcontainers.shaded.org.apache.commons.lang.RandomStringUtils;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClientBuilder;

import javax.annotation.PostConstruct;
import java.net.URI;
import java.util.Collections;
import java.util.concurrent.ExecutionException;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@Slf4j
public class KfKeyManagementApplicationTests extends AbstractTest {

    public static final String CLIENT_SECRET = "secret";
    public static final String REALM_NAME = "master";

    @MockBean
    private KMSService kmsService;

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

    private final String fenceAuthClientUri = "/auth-client";
    private final String fenceTokenUri = "/token";
    private final String fenceRefreshUri = "/refresh";
    private final String keyStoreUri = "/key-store";
    private final static String KF_CLIENT_ID = "kf";
    private static DynamoDbEnhancedAsyncClient dynamoClient;
    private static String defaultCavaticaAccessToken = "";
    private static DynamoDbAsyncTable<Secret> secretTable;

    @BeforeAll
    static void init() {
        DynamoDbAsyncClientBuilder builder = DynamoDbAsyncClient.builder();
        builder.credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test"))).region(Region.US_EAST_1)
                .endpointOverride(URI.create("http://localhost:" + dynamodb.getFirstMappedPort()));
        dynamoClient = DynamoDbEnhancedAsyncClient.builder().dynamoDbClient(builder.build())
                .build();

        try {
            secretTable = dynamoClient.table("kf-key-management-secret", TableSchema.fromBean(Secret.class));
            secretTable.createTable().get();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        createKeycloakClient(KF_CLIENT_ID);
        val userAndToken = createUserAndSecretAndObtainAccessToken("cavatica", "cavatica_secret");
        defaultCavaticaAccessToken = userAndToken.getAccessToken();

    }


    private static UserIdAndToken createUserAndSecretAndObtainAccessToken(String service, String secret) {
        val username = RandomStringUtils.random(10, true, false);
        val password = RandomStringUtils.random(10, true, false);
        val userId = createKeycloakUser(username, password, "test" + username + "@test.org", RandomStringUtils.random(10, true, false), RandomStringUtils.random(10, true, false));
        createSecret(service, userId, secret);
        String accessToken = obtainAccessToken(username, password);

        return new UserIdAndToken(userId, accessToken);
    }

    @PostConstruct
    private void setup() {
        given(kmsService.encrypt(any())).willAnswer(invocation -> "encrypted_" + invocation.getArgument(0));
        given(kmsService.decrypt(any())).willAnswer(invocation -> "decrypted_" + invocation.getArgument(0));

    }

    public static void createSecret(String service, String userId, String secret) {
        dynamoClient.table("kf-key-management-secret", TableSchema.fromBean(Secret.class)).putItem(new Secret(userId, service, secret));
    }

    public static String createKeycloakUser(String username, String password, String email, String firstName, String lastName) {
        UserRepresentation user = new UserRepresentation();
        user.setEnabled(true);
        user.setUsername(username);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmail(email);

        CredentialRepresentation passwordCred = new CredentialRepresentation();
        passwordCred.setTemporary(false);
        passwordCred.setType(CredentialRepresentation.PASSWORD);
        passwordCred.setValue(password);

        user.setCredentials(Collections.singletonList(passwordCred));
        javax.ws.rs.core.Response response = keycloak.getKeycloakAdminClient().realm(REALM_NAME).users().create(user);
        return CreatedResponseUtil.getCreatedId(response);
    }

    public static void createKeycloakClient(String clientId) {
        ClientRepresentation client = new ClientRepresentation();
        client.setClientId(clientId);
        client.setEnabled(true);
        client.setPublicClient(false);
        client.setDirectAccessGrantsEnabled(true);
        client.setRedirectUris(Collections.singletonList("*"));
        client.setSecret(CLIENT_SECRET);
        keycloak.getKeycloakAdminClient().realm(REALM_NAME).clients().create(client);
    }

    private static String obtainAccessToken(String username, String password) {
        String tokenUrl = keycloak.getAuthServerUrl() + "realms/master/protocol/openid-connect/token";
        Response r = RestAssured.given()
                .contentType("application/x-www-form-urlencoded; charset=utf-8")
                .formParam("grant_type", "password")
                .formParam("username", username)
                .formParam("password", password)
                .formParam("client_id", KfKeyManagementApplicationTests.KF_CLIENT_ID)
                .formParam("client_secret", CLIENT_SECRET)
                .post(tokenUrl);

        assertThat(HttpStatus.OK.value()).isEqualTo(r.statusCode());
        return r.body().jsonPath().get("access_token");

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
                .header("Authorization", "Bearer " + defaultCavaticaAccessToken)
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
                .header("Authorization", "Bearer " + defaultCavaticaAccessToken)
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
                .header("Authorization", "Bearer " + defaultCavaticaAccessToken)
                .bodyValue(content.toJSONString())
                .exchange()
                .expectStatus().isEqualTo(400);
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
                .header("Authorization", "Bearer " + defaultCavaticaAccessToken)

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
                .header("Authorization", "Bearer " + defaultCavaticaAccessToken)

                .exchange()
                .expectStatus().is2xxSuccessful()
                .expectBody().jsonPath("$.scope").exists()
                .json("{\"scope\":\"openid\",\"redirect_uri\":\"https://portal.kidsfirstdrc.org/gen3_redirect/\",\"client_id\":\"gen3_client_id\"}");
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
                .header("Authorization", "Bearer " + defaultCavaticaAccessToken)
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
                .header("Authorization", "Bearer " + defaultCavaticaAccessToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody().returnResult().getResponseBody();
        assert body != null;
        assertThat(new String(body)).isEqualTo("decrypted_cavatica_secret");
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
        val secret = secretTable.getItem(new Secret(userIdAndToken.getUserId(), "cavatica", null)).get();
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

        val secret = secretTable.getItem(new Secret(userIdAndToken.getUserId(), "cavatica", null)).get();
        assertThat(secret).isNotNull();
        assertThat(secret.getSecret()).isEqualTo("encrypted_" + my_secret);
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
        val userIdAndToken = createUserAndSecretAndObtainAccessToken("fence_gen3_refresh", "secret");
        JSONObject content = new JSONObject();
        content.put("access_token", "this_is_access_token");
        content.put("refresh_token", "this_is_refresh_token");
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
                .jsonPath("$.refresh_token").value(o -> assertThat(o).isEqualTo("this_is_refresh_token"));

        val accessSecret = secretTable.getItem(new Secret(userIdAndToken.getUserId(), "fence_gen3_access", null)).get();
        assertThat(accessSecret).isNotNull();
        assertThat(accessSecret.getSecret()).isEqualTo("encrypted_this_is_access_token");

        val refreshSecret = secretTable.getItem(new Secret(userIdAndToken.getUserId(), "fence_gen3_refresh", null)).get();
        assertThat(refreshSecret).isNotNull();
        assertThat(refreshSecret.getSecret()).isEqualTo("encrypted_this_is_refresh_token");

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
        val accessSecret = secretTable.getItem(new Secret(userIdAndToken.getUserId(), "fence_gen3_access", null)).get();
        assertThat(accessSecret).isNull();

        val refreshSecret = secretTable.getItem(new Secret(userIdAndToken.getUserId(), "fence_gen3_refresh", null)).get();
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
                .jsonPath("$.access_token").value(o -> assertThat(o).isEqualTo("decrypted_this_is_access_token"));

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

        val accessSecret = secretTable.getItem(new Secret(userIdAndToken.getUserId(), "fence_gen3_access", null)).get();
        assertThat(accessSecret).isNotNull();
        assertThat(accessSecret.getSecret()).isEqualTo("encrypted_this_is_access_token");

        val refreshSecret = secretTable.getItem(new Secret(userIdAndToken.getUserId(), "fence_gen3_refresh", null)).get();
        assertThat(refreshSecret).isNotNull();
        assertThat(refreshSecret.getSecret()).isEqualTo("encrypted_this_is_refresh_token");

    }

}
