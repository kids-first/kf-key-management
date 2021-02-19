package io.kidsfirst.core.service;

import io.jsonwebtoken.*;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.codec.binary.Base64;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Map;

@Service
@Slf4j
public class JWTService {

  private Environment env;

  public JWTService(Environment env) {
    this.env = env;
  }

  public String getUserId(Jwt token) {
    // Property 'sub' contains userId:
    val body  = (Map)    token.getBody();
    val id    = (String) body.get("sub");

    return id;
  }

  public PublicKey getPublicKey(String environment) {
    String publicKeyString;

    switch (environment) {
      case "ego":
        publicKeyString = env.getProperty("application.ego_public", env.getProperty("ego_public"));
        break;
      case "fence":
        publicKeyString = env.getProperty("application.fence_public", env.getProperty("fence_public"));
        break;
      default:
        return null;
    }

    val publicBytes = Base64.decodeBase64(publicKeyString);
    val keySpec = new X509EncodedKeySpec(publicBytes);
    try {
      val keyFactory = KeyFactory.getInstance("RSA");
      return keyFactory.generatePublic(keySpec);

    } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
      // Shouldn't be reached, but handle anyways
      log.error(e.getMessage(), e);
      return null;
    }
  }

  public Jwt parseToken(String token, String environment) throws
          SignatureException, ExpiredJwtException, MalformedJwtException, UnsupportedJwtException {
    val key = getPublicKey(environment);

    val jwt = Jwts.parser()
            .setSigningKey(key)
            .parse(token);

    return jwt;
  }
}
