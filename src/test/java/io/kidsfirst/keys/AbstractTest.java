package io.kidsfirst.keys;

import org.json.simple.parser.JSONParser;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.FixedHostPortGenericContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Arrays;
import java.util.Collection;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
public abstract class AbstractTest {

    private static final int DYNAMODB_PORT = 8000;
    private static final int KEYCLOAK_PORT_HTTP = 8123;

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected WebApplicationContext webApplicationContext;

    @Autowired
    protected Environment env;

    protected JSONParser jsonParser = new JSONParser();

    protected void assertArraysEqualIgnoreOrder(Object[] expected, Object[] actual) {
        assertArraysEqualIgnoreOrder(Arrays.asList(expected), Arrays.asList(actual));
    }

    protected void assertArraysEqualIgnoreOrder(Collection expected, Collection actual) {
        Assertions.assertTrue(
                expected.size() == actual.size() &&
                        expected.containsAll(actual) &&
                        actual.containsAll(expected)
        );
    }

    @Container
    public static GenericContainer dynamodb = new GenericContainer<>("amazon/dynamodb-local:latest")
            .withExposedPorts(DYNAMODB_PORT);

    @Container  // need to use it even if deprecated because need to know which port to use before runtime for configuration file
    public static GenericContainer keycloak = new FixedHostPortGenericContainer("quay.io/keycloak/keycloak:13.0.1")
            .withFixedExposedPort(KEYCLOAK_PORT_HTTP, 8080)
            .withEnv("KEYCLOAK_USER", "admin")
            .withEnv("KEYCLOAK_PASSWORD", "admin");

}
