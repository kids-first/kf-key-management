package io.kidsfirst.keys;

import org.json.simple.parser.JSONParser;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import java.util.Arrays;
import java.util.Collection;

@SpringBootTest
@AutoConfigureMockMvc
//@Testcontainers
public abstract class AbstractTest {

    private static final int DYNAMODB_PORT = 8000;

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected WebApplicationContext webApplicationContext;

    @Autowired
    protected Environment env;

    protected JSONParser jsonParser = new JSONParser();

    protected void assertArraysEqualIgnoreOrder(Object[] expected, Object[] actual){
        assertArraysEqualIgnoreOrder(Arrays.asList(expected), Arrays.asList(actual));
    }

    protected void assertArraysEqualIgnoreOrder(Collection expected, Collection actual){
        Assertions.assertTrue(
                expected.size() == actual.size() &&
                        expected.containsAll(actual) &&
                        actual.containsAll(expected)
        );
    }

    /*@Container
    public static GenericContainer dynamodb = new GenericContainer<>("amazon/dynamodb-local:latest")
                                                        .withExposedPorts(DYNAMODB_PORT);*/
}
