package io.kidsfirst.keys;

import com.nimbusds.jose.shaded.json.JSONObject;
import io.kidsfirst.core.model.Secret;
import io.kidsfirst.core.service.KMSService;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.CreatedResponseUtil;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClientBuilder;

import javax.annotation.PostConstruct;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.function.Consumer;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@Slf4j
public class KfKeyManagementApplicationTests extends AbstractTest {

    public static final String CLIENT_SECRET = "secret";
    public static final String REALM_NAME = "master";

    @MockBean
    private KMSService kmsService;

    private static final String encryptedString = "encryptedSecret";
    private static final String decryptedString = "decryptedSecret";

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
    private final String refreshTokenValue = "refreshTokenValue";
    private final String accessTokenValue = "accessTokenValue";
    private final String OICDJwtTokenValue = "OICDJwtTokenValue";
    private static DynamoDbEnhancedAsyncClient dynamoClient;
    private String accessToken = "";

    @BeforeAll
    static void init() {
        DynamoDbAsyncClientBuilder builder = DynamoDbAsyncClient.builder();
        builder.credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test"))).region(Region.US_EAST_1)
                .endpointOverride(URI.create("http://localhost:" + dynamodb.getFirstMappedPort()));
        dynamoClient = DynamoDbEnhancedAsyncClient.builder().dynamoDbClient(builder.build())
                .build();

        try {
            dynamoClient.table("kf-key-management-secret", TableSchema.fromBean(Secret.class)).createTable().get();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        createKeycloakClient();
        val userId = createKeycloakUser("test", "test", "test@test.org", "John", "Doe");
        createSecret("cavatica", userId, "cavatica_secret");


        /*String userId1 = RandomString.make(10);
        val secret1 = new Secret(userId1,"cavatica", RandomString.make(10));
        String userId2 = RandomString.make(10);
        val secret2 = new Secret(userId2,"cavatica", RandomString.make(10));

		given(secretService.getSecret("cavatica", userId1)).willReturn(Mono.just(secret1));
		given(secretService.getSecret("cavatica", userId2)).willReturn(Mono.just(secret2));
		given(kmsService.encrypt(any())).willReturn(encryptedString);
		given(kmsService.decrypt(any())).willReturn(decryptedString);


		try {
			given(cavaticaService.sendCavaticaRequest(any(), any(), any(), any())).willReturn(Mono.just(cavaticaResponseBody));

			AccessToken accessToken = new AccessToken(AccessTokenType.BEARER, accessTokenValue) {
				@Override
				public String toAuthorizationHeader() {
					return this.getType().getValue() + " " + this.getValue();
				}
			};
			RefreshToken refreshToken = new RefreshToken(refreshTokenValue);

			Tokens tokens = new Tokens(accessToken, refreshToken);
			OIDCTokens oidcTokens = new OIDCTokens(OICDJwtTokenValue, accessToken, refreshToken);

			given(fenceService.refreshTokens(any(), any())).willReturn(Mono.just(tokens));
			given(fenceService.requestTokens(any(), any())).willReturn(Mono.just(oidcTokens));
			given(fenceService.getProvider(any())).willCallRealMethod();
		} catch (Exception e) {
			// Mocked - it will not throw any exception.
			log.error("Should never get here.", e);
		}*/
    }

    @PostConstruct
    private void setup() {
        given(kmsService.encrypt(any())).willAnswer(invocation -> "encrypted_" + invocation.getArgument(0));
        given(kmsService.decrypt(any())).willAnswer(invocation -> "decrypted_" + invocation.getArgument(0));

                //willReturn().willReturn(decryptedString);
        accessToken = obtainAccessToken("test", "test");
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

    private static void createKeycloakClient() {
        ClientRepresentation client = new ClientRepresentation();
        client.setClientId("kf");
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
                .formParam("client_id", "kf")
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
                .bodyValue(body.toJSONString())
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
        cavticaWM.stubFor(get("/user").willReturn(ok(cavaticaResponseBody)));
        webClient.post()
                .uri("/cavatica")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + accessToken)
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

        cavticaWM.stubFor(get("/user").willReturn(ok(cavaticaResponseBody)));
        webClient.post()
                .uri("/cavatica")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + accessToken)
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
        cavticaWM.stubFor(get("/user").willReturn(ok(cavaticaResponseBody)));
        webClient.post()
                .uri("/cavatica")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + accessToken)
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
                .expectHeader().value("Allow", matchValues("GET","HEAD","OPTIONS"));

    }

    @Test
    void testFenceDCFAuthClientGET() {
        webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(fenceAuthClientUri)
                        .queryParam("fence", "dcf")
                        .build())
                .header("Authorization", "Bearer " + accessToken)

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
                .header("Authorization", "Bearer " + accessToken)

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
                .header("Authorization", "Bearer " + accessToken)
                .exchange()
                .expectStatus().isEqualTo(406)
                .expectHeader();

    }

    @Test
    void testKeyStoreGET() {
       val body= webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(keyStoreUri)
                        .queryParam("service", "cavatica")
                        .build())
                .accept(MediaType.TEXT_PLAIN)
                .header("Authorization", "Bearer " + accessToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody().returnResult().getResponseBody();
        assert body != null;
        assertThat(new String(body)).isEqualTo("decrypted_cavatica_secret");
    }
/*
    @Test
    void testKeyStoreDELETE() throws Exception {
        JSONObject body = new JSONObject();
        body.put("service", "cavatica");

        MvcResult result = super.mvc.perform(
                MockMvcRequestBuilders.delete(keyStoreUri)
                        .header("Authorization", "Bearer " + accessToken)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body.toJSONString())
        ).andReturn();

        int status = result.getResponse().getStatus();
        Assertions.assertEquals(200, status);
    }

    @Test
    void testKeyStorePUT() throws Exception {
        JSONObject body = new JSONObject();
        body.put("service", "cavatica");
        body.put("secret", "60ebf2b87bba49a2f932c8c7a8daa639");

        MvcResult result = super.mvc.perform(
                MockMvcRequestBuilders.put(keyStoreUri)
                        .header("Authorization", "Bearer " + accessToken)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body.toJSONString())
        ).andReturn();

        int status = result.getResponse().getStatus();
        Assertions.assertEquals(200, status);
    }

    @Test
    void testFenceRefreshPreflight() throws Exception {
        MvcResult result = super.mvc.perform(
                MockMvcRequestBuilders.options(fenceRefreshUri)
        ).andReturn();

        String[] allowedMethods = ((String) result.getResponse().getHeaderValue("Allow")).split(",");
        this.assertArraysEqualIgnoreOrder(new String[]{"POST", "OPTIONS"}, allowedMethods);

        int status = result.getResponse().getStatus();
        Assertions.assertEquals(200, status);
    }

    @Test
    void testFenceGEN3RefreshPOST() throws Exception {
        MvcResult result = super.mvc.perform(
                MockMvcRequestBuilders.post(fenceRefreshUri)
                        .header("Authorization", "Bearer " + accessToken)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .queryParam("fence", "gen3")
        ).andReturn();

        int status = result.getResponse().getStatus();
        Assertions.assertEquals(200, status);

        JSONObject response = (JSONObject) this.jsonParser.parse(result.getResponse().getContentAsString());
        Assertions.assertNotNull(response.get("access_token"));
        Assertions.assertNotNull(response.get("refresh_token"));
    }

    @Test
    void testFenceDCFRefreshPOST() throws Exception {
        MvcResult result = super.mvc.perform(
                MockMvcRequestBuilders.post(fenceRefreshUri)
                        .header("Authorization", "Bearer " + accessToken)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .queryParam("fence", "dcf")
        ).andReturn();

        int status = result.getResponse().getStatus();
        Assertions.assertEquals(200, status);

        JSONObject response = (JSONObject) this.jsonParser.parse(result.getResponse().getContentAsString());
        Assertions.assertNotNull(response.get("access_token"));
        Assertions.assertNotNull(response.get("refresh_token"));
    }

    @Test
    void testInvalidFenceRefreshPOST() throws Exception {
        MvcResult result = super.mvc.perform(
                MockMvcRequestBuilders.post(fenceRefreshUri)
                        .header("Authorization", "Bearer " + accessToken)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .queryParam("fence", "unknown")
        ).andReturn();

        // Expect bad request
        int status = result.getResponse().getStatus();
        Assertions.assertEquals(400, status);
    }

    @Test
    void testFenceTokenPreflight() throws Exception {
        MvcResult result = super.mvc.perform(
                MockMvcRequestBuilders.options(fenceTokenUri)
        ).andReturn();

        String[] allowedMethods = ((String) result.getResponse().getHeaderValue("Allow")).split(",");
        this.assertArraysEqualIgnoreOrder(new String[]{"GET", "HEAD", "DELETE", "POST", "OPTIONS"}, allowedMethods);

        int status = result.getResponse().getStatus();
        Assertions.assertEquals(200, status);
    }

    @Test
    void testFenceTokenDELETE() throws Exception {
        MvcResult result = super.mvc.perform(
                MockMvcRequestBuilders.delete(fenceTokenUri)
                        .header("Authorization", "Bearer " + accessToken)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .queryParam("fence", "gen3")
        ).andReturn();

        int status = result.getResponse().getStatus();
        Assertions.assertEquals(200, status);
    }

    @Test
    void testFenceDCFTokenGET() throws Exception {
        MvcResult result = super.mvc.perform(
                MockMvcRequestBuilders.get(fenceTokenUri)
                        .header("Authorization", "Bearer " + accessToken)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .queryParam("fence", "dcf")
        ).andReturn();

        int status = result.getResponse().getStatus();
        Assertions.assertEquals(200, status);

        JSONObject response = (JSONObject) this.jsonParser.parse(result.getResponse().getContentAsString());
        Assertions.assertNotNull(response.get("access_token"));
    }

    @Test
    void testFenceGEN3TokenGET() throws Exception {
        MvcResult result = super.mvc.perform(
                MockMvcRequestBuilders.get(fenceTokenUri)
                        .header("Authorization", "Bearer " + accessToken)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .queryParam("fence", "gen3")
        ).andReturn();

        int status = result.getResponse().getStatus();
        Assertions.assertEquals(200, status);

        JSONObject response = (JSONObject) this.jsonParser.parse(result.getResponse().getContentAsString());
        Assertions.assertNotNull(response.get("access_token"));
    }

    @Test
    void testFenceTokenPOST() throws Exception {
        MvcResult result = super.mvc.perform(
                MockMvcRequestBuilders.post(fenceTokenUri)
                        .header("Authorization", "Bearer " + accessToken)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .queryParam("fence", "gen3")
                        .queryParam("code", "anAuthCodeValue")
        ).andReturn();

        int status = result.getResponse().getStatus();
        Assertions.assertEquals(200, status);

        JSONObject response = (JSONObject) this.jsonParser.parse(result.getResponse().getContentAsString());
        Assertions.assertNotNull(response.get("access_token"));
    }
*/
}
