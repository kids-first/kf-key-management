package io.kidsfirst.web.rest;

import com.nimbusds.jose.shaded.json.JSONObject;
import io.kidsfirst.core.model.Secret;
import io.kidsfirst.core.service.SecretService;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/key-store")
public class KeysResource {

    private final SecretService secretService;

    public KeysResource(SecretService secretService){
        this.secretService = secretService;
    }

    @GetMapping(produces = MediaType.TEXT_PLAIN_VALUE)
    public Mono<String> getSecret(@RequestParam("service") String service, JwtAuthenticationToken authentication){
        val userId = authentication.getTokenAttributes().get("sub").toString();

        return secretService.fetchAndDecrypt(userId, service);
    }

    @PutMapping
    public Mono<Void> saveSecret(@RequestBody JSONObject body, JwtAuthenticationToken authentication){
        val userId = authentication.getTokenAttributes().get("sub").toString();

        // === 1. Get service and secretValue from event
        val service =  (String)body.get("service");
        val secretValue =  (String)body.get("secret");

        // === 2. Create a Secret to hold the data
        val secret = new Secret(userId, service, secretValue);

        // === 3. Save to dynamo DB
        return secretService.encryptAndSave(secret).then();


    }

    @DeleteMapping
    public Mono<Void> deleteSecret(@RequestBody JSONObject body, JwtAuthenticationToken authentication){
        val userId = authentication.getTokenAttributes().get("sub").toString();
        val service =  (String)body.get("service");

        return secretService.deleteSecret(service, userId).then();

    }


}