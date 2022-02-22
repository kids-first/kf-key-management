package io.kidsfirst.keys;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import dasniko.testcontainers.keycloak.KeycloakContainer;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Arrays;
import java.util.function.Consumer;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

@ActiveProfiles("dev")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@AutoConfigureWebTestClient(timeout = "5000")
public abstract class AbstractTest{

    private static final int DYNAMODB_PORT = 8000;

    @Autowired
    protected Environment env;

    @Autowired
    protected WebTestClient webClient;


    @RegisterExtension
    static WireMockExtension cavaticaWM = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @RegisterExtension
    static WireMockExtension gen3VM = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    protected Consumer<String> matchValues(String... others) {
        return s -> MatcherAssert.assertThat(Arrays.asList(s.split(",")), Matchers.hasItems(others));
    }


    @Container
    public static GenericContainer<?> dynamodb = new GenericContainer<>("amazon/dynamodb-local:latest")
            .withExposedPorts(DYNAMODB_PORT);

    @Container
    public static KeycloakContainer keycloak = new KeycloakContainer()
            .withAdminUsername("admin")
            .withAdminPassword("admin");

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

    @DynamicPropertySource
    static void setCavaticaUri(DynamicPropertyRegistry registry) {
        registry.add("application.cavatica_root",
                () -> cavaticaWM.baseUrl());
    }

    @DynamicPropertySource
    static void setGen3Uri(DynamicPropertyRegistry registry) {
        registry.add("application.fence.gen3.token_endpoint",
                () -> gen3VM.baseUrl());
    }





}
