package io.kidsfirst.web.rest;

import com.nimbusds.oauth2.sdk.ParseException;
import io.kidsfirst.core.exception.NotFoundException;
import io.kidsfirst.core.model.Provider;
import io.kidsfirst.core.service.FenceService;
import io.kidsfirst.core.service.SecretService;
import io.kidsfirst.core.utils.Timed;
import lombok.val;
import org.json.simple.JSONObject;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URISyntaxException;

@RestController
@RequestMapping("/")
public class FenceResource {
    
    private FenceService fenceService;
    private SecretService secretService;
    
    public FenceResource(FenceService fenceService, SecretService secretService){
        this.fenceService = fenceService;
        this.secretService = secretService;
    }

    @Timed
    @GetMapping("/auth-client")
    public ResponseEntity<JSONObject> getAuthClient(@RequestParam("fence") String fenceKey) throws IllegalArgumentException {
        //No UserID check - no auth required
        val fence = fenceService.getProvider(fenceKey);
        val body = new JSONObject();

        body.put("client_id", fence.getClientId());
        body.put("redirect_uri", fence.getRedirectUri());
        body.put("scope", fence.getScope());

        // DO NOT RETURN SECRET >:O
        return ResponseEntity.ok(body);
    }

    @Timed
    @PostMapping("/refresh")
    public ResponseEntity<JSONObject> refresh(@RequestParam("fence") String fenceKey)throws IllegalArgumentException, ParseException, IOException, URISyntaxException {
        val userId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        val fence = fenceService.getProvider(fenceKey);
        val storedRefresh = secretService.fetchRefreshToken(fence, userId);

        if(storedRefresh.isPresent()) {
            val refresh = storedRefresh.get();
            val tokensResponse = fenceService.refreshTokens(refresh, fence);

            if(tokensResponse.isPresent()) {
                val tokens = tokensResponse.get();
                secretService.persistAccessToken(fence, userId, tokens.getAccessToken().getValue());
                secretService.persistRefreshToken(fence, userId, tokens.getRefreshToken().getValue());

                val body = new JSONObject();
                body.put("access_token", tokens.getAccessToken().getValue());
                body.put("refresh_token", tokens.getRefreshToken().getValue());

                return ResponseEntity.ok(body);
            }
            throw new IllegalArgumentException("Fence failed refresh attempt.");
        }
        throw new IllegalArgumentException("Requested user has no stored refresh token.");
    }

    @Timed
    @GetMapping("/token")
    public ResponseEntity<JSONObject> getTokens(@RequestParam("fence") String fenceKey) throws IllegalArgumentException, NotFoundException {
        val userId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        val fence = fenceService.getProvider(fenceKey);

        val accessToken = secretService.fetchAccessToken(fence, userId);
        val refreshToken = secretService.fetchRefreshToken(fence, userId);

        if (!accessToken.isPresent() || !refreshToken.isPresent()) {
            throw new NotFoundException(String.format("No token for Fence: %s", fenceKey));
        }

        val body = new JSONObject();
        body.put("access_token", accessToken.orElse(""));
        body.put("refresh_token", refreshToken.orElse(""));

        return ResponseEntity.ok(body);
    }

    @Timed
    @PostMapping("/token")
    public ResponseEntity<JSONObject> requestTokens(@RequestParam("code") String authCode, @RequestParam("fence") String fenceKey) throws IllegalAccessException, IllegalArgumentException, ParseException, IOException, URISyntaxException {
        val userId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        val fence = fenceService.getProvider(fenceKey);
        val tokenResponse = fenceService.requestTokens(authCode, fence);

        if(tokenResponse.isPresent()) {
            val tokens = tokenResponse.get();
            secretService.persistTokens(fence, userId, tokens);

            val body = new JSONObject();
            body.put("access_token", tokens.getAccessToken().getValue());
            body.put("refresh_token", tokens.getRefreshToken().getValue());

            return ResponseEntity.ok(body);
        }
        throw new IllegalAccessException("Fence did not return tokens for the provided code.");
    }

    @Timed
    @DeleteMapping("/token")
    public ResponseEntity deleteToken(@RequestParam("fence") String fenceKey) throws IllegalArgumentException {
        val userId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Provider fence = fenceService.getProvider(fenceKey);
        secretService.removeFenceTokens(fence, userId);

        return ResponseEntity.ok().build();
    }
}
