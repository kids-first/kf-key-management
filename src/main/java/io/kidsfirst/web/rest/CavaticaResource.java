package io.kidsfirst.web.rest;

import com.nimbusds.jose.shaded.json.JSONObject;
import io.kidsfirst.core.service.CavaticaService;
import io.kidsfirst.core.service.SecretService;
import lombok.val;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.Map;

@RestController
@RequestMapping("/cavatica")
public class CavaticaResource {

    private final SecretService secretService;
    private final CavaticaService cavaticaService;

    private final String[] HTTP_ALLOWED_METHODS = new String[]{"GET", "POST", "PUT", "PATCH", "DELETE"};

    public CavaticaResource(SecretService secretService,  CavaticaService cavaticaService) {
        this.secretService = secretService;
        this.cavaticaService = cavaticaService;
    }

    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<String> cavatica(@RequestBody(required = false) JSONObject requestBody, JwtAuthenticationToken authentication) {
        val userId = authentication.getTokenAttributes().get("sub").toString();
        val cavaticaKey = getCavaticaKey(userId);

        // Path
        val path = (String) requestBody.get("path");
        if (path == null) {
            throw new IllegalArgumentException("No Parameter found for 'path' in body.");
        }

        // Method
        String method = ((String) requestBody.get("method")).toUpperCase();
        if (Arrays.stream(HTTP_ALLOWED_METHODS).noneMatch(allowed -> allowed.equals(method))) {
            // Invalid method provided
            throw new IllegalArgumentException(String.format("Provided method '%s' is not allowed.", method));
        }

        // Body
        val bodyMap = (Map) requestBody.get("body");
        val body = bodyMap != null ? new JSONObject(bodyMap) : null;
        val bodyString = body == null ? "" : body.toJSONString();

        return cavaticaKey.flatMap(key -> cavaticaService.sendCavaticaRequest(key, path, method, bodyString));
    }

    private Mono<String> getCavaticaKey(String userId) throws IllegalArgumentException {
        return secretService.fetchAndDecrypt(userId, "cavatica");

    }


}
