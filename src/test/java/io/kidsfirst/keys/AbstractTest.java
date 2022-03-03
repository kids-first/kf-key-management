package io.kidsfirst.keys;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import io.kidsfirst.core.model.Secret;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import lombok.val;
import org.apache.commons.io.IOUtils;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.keycloak.admin.client.CreatedResponseUtil;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.shaded.org.apache.commons.lang.RandomStringUtils;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClientBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.function.Consumer;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@ActiveProfiles("dev")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient(timeout = "5000")
public abstract class AbstractTest {

    public static final String CLIENT_SECRET = "secret";
    public static final String REALM_NAME = "master";
    protected static final int DYNAMODB_PORT = 8000;
    protected final static String KF_CLIENT_ID = "kf";
    protected static DynamoDbEnhancedAsyncClient dynamoClient;
    protected static DynamoDbAsyncTable<Secret> secretTable;
    public static GenericContainer<?> dynamodb = new GenericContainer<>("amazon/dynamodb-local:latest")
            .withExposedPorts(DYNAMODB_PORT);

    public static KeycloakContainer keycloak = new KeycloakContainer()
            .withAdminUsername("admin")
            .withAdminPassword("admin");

    static {
        keycloak.start();
        dynamodb.start();
        init();
    }

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

    }

    @Autowired
    protected Environment env;

    @Autowired
    protected WebTestClient webClient;

    @DynamicPropertySource
    static void setDynamicKeycloakPort(DynamicPropertyRegistry registry) {
        registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri",
                () -> String.format("http://localhost:%d/realms/master", keycloak.getHttpPort()));
    }

    @DynamicPropertySource
    static void setDynamicDynamodbPort(DynamicPropertyRegistry registry) {
        registry.add("aws.dynamodb.endpoint",
                () -> String.format("http://localhost:%d", dynamodb.getFirstMappedPort()));
    }

    protected Consumer<String> matchValues(String... others) {
        return s -> MatcherAssert.assertThat(Arrays.asList(s.split(",")), Matchers.hasItems(others));
    }

    protected static UserIdAndToken createUserAndSecretAndObtainAccessToken(String service, String secret, Long expiration) {
        val username = RandomStringUtils.random(10, true, false);
        val password = RandomStringUtils.random(10, true, false);
        val userId = createKeycloakUser(username, password, "test" + username + "@test.org", RandomStringUtils.random(10, true, false), RandomStringUtils.random(10, true, false));
        createSecret(service, userId, secret, expiration);
        String accessToken = obtainAccessToken(username, password);

        return new UserIdAndToken(userId, accessToken);
    }
    protected static UserIdAndToken createUserAndSecretAndObtainAccessToken(String service, String secret) {
        return createUserAndSecretAndObtainAccessToken(service, secret, null);
    }

    public static void createSecret(String service, String userId, String secret) {
        val expiration = Instant.now().plus(1, ChronoUnit.HOURS).getEpochSecond();
        createSecret(service,userId,secret, expiration);
    }
    public static void createSecret(String service, String userId, String secret, Long expiration) {
        dynamoClient.table("kf-key-management-secret", TableSchema.fromBean(Secret.class)).putItem(new Secret(userId, service, secret, expiration));

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

    protected static String obtainAccessToken(String username, String password) {
        String tokenUrl = keycloak.getAuthServerUrl() + "realms/master/protocol/openid-connect/token";
        Response r = RestAssured.given()
                .contentType("application/x-www-form-urlencoded; charset=utf-8")
                .formParam("grant_type", "password")
                .formParam("username", username)
                .formParam("password", password)
                .formParam("client_id", KF_CLIENT_ID)
                .formParam("client_secret", CLIENT_SECRET)
                .post(tokenUrl);

        assertThat(HttpStatus.OK.value()).isEqualTo(r.statusCode());
        return r.body().jsonPath().get("access_token");

    }

    public String contentFromResource(String name) throws IOException {
        InputStream resourceAsStream = this.getClass().getResourceAsStream(name);
        assert resourceAsStream != null;
        return IOUtils.toString(resourceAsStream, StandardCharsets.UTF_8);
    }

}
