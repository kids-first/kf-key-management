package io.kidsfirst.keys;

import io.jsonwebtoken.Jwts;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

public class AuthUtils {

    private static final String privateKeyString =
            "MIIEvAIBADANBgkqhkiG9w0BAQEFAASCBKYwggSiAgEAAoIBAQCE8dNulSCnJFMU" +
            "IVPOUoZn3+SBcRHvRKyI2thXkHnNpPzb4+NIddf6zUyXBm6OjRyNP6F8w/UFWiN+" +
            "qZUgzJLevVXD274f/K3gGckv7WhPTBnM0lhVXrszTeX4AjR+eqQ+S/vtLXYraoWm" +
            "9/LsNo7tzSEsgcsEO/74HCS7DVDCn9lpMUcwOJExwObwWNYaGSZjMfwD86MEMZK+" +
            "Surw4RdKim2IMP1fik9xRV19U90zfwl+ObEeL2TWsZ0IeUpJs4daWnj/GL4vfG4V" +
            "Gp15u46VUXopQStv5rblFa6kYx71E7nKHsOjtIJ++f1RYFqbCkrnoPXbJojAmI+W" +
            "XpSYnIUTAgMBAAECggEAQsmqSneou89H3WkQzSowU6brCQxg0i9R7j7jSLpQNruY" +
            "PG+0EwcAxgK1Y3nbuMmzelJpMmDPiyzJgCx5usyLTFMZ7xdJ4I/0Wg0aQXWnXY8B" +
            "tyBFOD4rFY8g5QNWk+PQ92r82R5pq04MbuFJrNmL3HOdItrhvvLVGJgq6724wM0I" +
            "Z498sh/ODCF1FQyWGogGhTZV5GEJ2rPa1X8z0ToITC87zVVyR5uL/sqX/FZlZGyP" +
            "FwS/vMoiLuuhmcuZLwYwVRE+PF+G4Esh9ERoQq3nbyYqnltqg7snbluwm5vPZhEC" +
            "+rmMgnvgwZp6hunsFQWpK6ud7MNua0opqTxbJoMZgQKBgQDgRvk58vGNUjSJLYBF" +
            "yFJ/aJKItus+5MlJV6LKXzfF+leoBW9bcP7h/Avb+r4KvPXdDWo8ZRD9x8higtqT" +
            "UO8ZnLi61F7c6VGyFInq62ta7L9h42c6aMo3u/FQ1BwXxIqi5BE6hzQfrmxQE1RG" +
            "BM1G+5zJ5wTxTX2LJad80MamIwKBgQCXv7hQaDG1fzLIg7onE83sBAJMnr7xLAV+" +
            "uvVP3ycs4Cw9f2M9vopHpqAe5Ok5W346wDcBAxCKAjUVCHTcBl0c8ZdEU/wSGwNP" +
            "je+tvgG/DJUBMO9dLQL38ug5LlOR473cfcOUxjjDn5rc6SqhXMeOsDt1YeEfp6nM" +
            "hhEJY6N8UQKBgGOBedApJtzRrTdztabAY0HeDq2ToroL3fapaDOrnV48XSnSB38l" +
            "miB/qG9YR3sSLW9/JTRYjvpZ6mEyt3GHBh5x91AFK3WOG04MaMiO3NnBkoQG1eUH" +
            "WzjxLPb0tOYisHPnBnHWTN0FkU7R1KEgPkeRGRZHZlz9SYc0FKR/KLPLAoGAES7f" +
            "cxNpVZDysivgX15puej2TbDIFE0UzjXjY0j0iatUtx3+odY6mERw6y6mjh0jHQn4" +
            "8H9lVwtK8XfEq8l4r7dXlqAf8fjnFhIPatASKI0HLlxZLmbTaDo0O41YXzO3owkG" +
            "pYkQkm43Pf6VGjKEUk74XKFZuZjlrEgeE78ZmxECgYA8PVn9mvWlui2gqMlDH6Su" +
            "mN/LQqx58VoOw+7ujnDEXUQ0aqk+TWKp+DyJlhyhROCMI3KzoeH2dByt0OwOUdqq" +
            "UHRulViQSxcYeYniwIvfInl9V8ngqqlKa9DG5W4PyqolPq1DNwWknZ86RlGzBGBr" +
            "7ZEXWb1xtL+9F5lQNmX6XQ==";

    public static String createRsaToken(String sub) {
        Instant now = Instant.now();

        return Jwts.builder()
                .setHeaderParam("kid", "myKeyId")
                .setHeaderParam("typ", "JWT")
                .setAudience("account")
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plus(5L, ChronoUnit.MINUTES)))
                .setIssuer("http://localhost:8123/auth/realms/master")
                .setSubject(sub)
                .setId(UUID.randomUUID().toString())
                .claim("typ", "Bearer")
                .signWith(getPrivateKey())
                .compact();
    }

    private static PrivateKey getPrivateKey(){
        try{
            KeyFactory kf = KeyFactory.getInstance("RSA");

            PKCS8EncodedKeySpec keySpecPKCS8 = new PKCS8EncodedKeySpec(Base64.getDecoder().decode(privateKeyString));
            return kf.generatePrivate(keySpecPKCS8);
        } catch(NoSuchAlgorithmException | InvalidKeySpecException ex){
            throw new RuntimeException(ex);
        }
    }
}
