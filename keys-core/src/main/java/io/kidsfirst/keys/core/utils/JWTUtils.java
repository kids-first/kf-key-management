package io.kidsfirst.keys.core.utils;

import io.jsonwebtoken.*;
import org.apache.commons.codec.binary.Base64;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Map;

public class JWTUtils {

  public static String getUserId(Jwt token) {
    // Property 'sub' contains userId:
    //   body.sub

    Map body     = (Map)    token.getBody();
    String id    = (String) body.get("sub");

    return id;
  }

  public static PublicKey getPublicKey(String environment) {

    String publicKeyString;
    switch (environment) {
      case "ego":
        publicKeyString = System.getenv("ego_public");
        break;
      case "fence":
        publicKeyString = System.getenv("fence_public");
        break;
      default:
        return null;
    }

    byte[] publicBytes = Base64.decodeBase64(publicKeyString);
    X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicBytes);
    try {
      KeyFactory keyFactory = KeyFactory.getInstance("RSA");
      return keyFactory.generatePublic(keySpec);

    } catch (NoSuchAlgorithmException e) {
      // TODO: Shouldn't be reached, but handle anyways
      return null;
    } catch (InvalidKeySpecException e) {
      // TODO: Shouldn't be reached, but handle anyways
      return null;
    }

  }

  public static Jwt parseToken(String token, String environment) throws
          SignatureException, ExpiredJwtException, MalformedJwtException, UnsupportedJwtException {

    PublicKey key = getPublicKey(environment);

    Jwt jwt = Jwts.parser()
            .setSigningKey(key)
            .parse(token);

    return jwt;
  }
}
