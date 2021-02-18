package io.kidsfirst.web.rest;

import io.kidsfirst.core.dao.SecretDao;
import io.kidsfirst.core.exception.NotFoundException;
import io.kidsfirst.core.model.Secret;
import io.kidsfirst.core.service.SecretService;
import io.kidsfirst.core.utils.Timed;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.json.simple.JSONObject;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/key-store")
public class KeysResource {

    private final SecretService secretService;
    private final SecretDao secretDao;

    public KeysResource(SecretService secretService, SecretDao secretDao){
        this.secretService = secretService;
        this.secretDao = secretDao;
    }

    @Timed
    @GetMapping(produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> getSecret(@RequestParam("service") String service){
        val userId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        val secretValue = secretService.fetchAndDecrypt(userId, service);

        if (!secretValue.isPresent()) {
            throw new NotFoundException(String.format("No value found for: %s", service));
        }

        return ResponseEntity.ok(secretValue.get());
    }

    @Timed
    @PutMapping
    public ResponseEntity saveSecret(@RequestBody JSONObject body){
        val userId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        // === 1. Get service and secretValue from event
        val service =  (String)body.get("service");
        val secretValue =  (String)body.get("secret");

        // === 2. Create a Secret to hold the data
        Secret secret = new Secret(userId, service, secretValue);

        // === 3. Save to dynamo DB
        secretService.encryptAndSave(secret);

        return ResponseEntity.ok().build();
    }

    @Timed
    @DeleteMapping
    public ResponseEntity deleteSecret(@RequestBody JSONObject body){
        val userId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        val service =  (String)body.get("service");

        secretDao.deleteSecret(service, userId);

        return ResponseEntity.ok().build();
    }
}