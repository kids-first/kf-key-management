package io.kidsfirst.web.rest;

import io.kidsfirst.core.model.Secret;
import io.kidsfirst.core.service.CavaticaService;
import io.kidsfirst.core.service.KMSService;
import io.kidsfirst.core.service.SecretService;
import io.kidsfirst.core.utils.Timed;
import lombok.val;
import org.json.simple.JSONObject;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/cavatica")
public class CavaticaResource {

    private SecretService secretService;
    private KMSService kmsService;
    private CavaticaService cavaticaService;
    private Environment env;

    private final String[] HTTP_ALLOWED_METHODS = new String[]{ "GET", "POST", "PUT", "PATCH", "DELETE" };

    public CavaticaResource(SecretService secretService, KMSService kmsService, CavaticaService cavaticaService, Environment env){
        this.secretService = secretService;
        this.kmsService = kmsService;
        this.cavaticaService = cavaticaService;
        this.env = env;
    }

    @Timed
    @PostMapping
    public ResponseEntity<String> cavatica(@RequestBody(required = false) JSONObject requestBody, HttpServletRequest request) throws IOException{
        val userId = (String)SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        val cavaticaKey = getCavaticaKey(userId);

        // Path
        val path = (String)requestBody.get("path");
        if (path == null) {
            throw new IllegalArgumentException(String.format("No Parameter found for 'path' in body."));
        }

        // Method
        String method = ((String)requestBody.get("method")).toUpperCase();
        if (Arrays.stream(HTTP_ALLOWED_METHODS).noneMatch(allowed -> allowed.equals(method))) {
            // Invalid method provided
            throw new IllegalArgumentException(String.format("Provided method '%s' is not allowed.", method));
        }

        // Body
        val bodyMap = (Map)requestBody.get("body");
        val body = bodyMap != null ? new JSONObject(bodyMap) : null;
        val bodyString = body == null ? null : body.toJSONString();

        val cavaticaResponse = cavaticaService.sendCavaticaRequest(cavaticaKey, path, method, bodyString);

        return ResponseEntity.ok(cavaticaResponse);
    }

    private String getCavaticaKey(String userId) throws IllegalArgumentException {
        List<Secret> allSecrets = secretService.getSecret("cavatica", userId);

        if (!allSecrets.isEmpty()) {
            Secret secret = allSecrets.get(0);
            String secretValue = secret.getSecret();

            return kmsService.decrypt(secretValue);

        } else {
            throw new IllegalArgumentException("No Cavatica token available; unable to send request.");
        }
    }
}
