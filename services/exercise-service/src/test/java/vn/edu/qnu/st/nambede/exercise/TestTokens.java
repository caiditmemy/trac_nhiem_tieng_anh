package vn.edu.qnu.st.nambede.exercise;

import io.jsonwebtoken.Jwts;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.time.Instant;
import java.util.Date;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import vn.edu.qnu.st.nambede.exercise.jwt.PublicKeys;

/** Builds tokens like the ones auth-service issues (RS256), signed with a key pair made for the tests. */
public final class TestTokens {

    public static final String KID = "test-key";
    public static final KeyPair KEYS = Jwts.SIG.RS256.keyPair().build();

    private TestTokens() {
    }

    public static String bearer(long userId, String role) {
        return "Bearer " + token(userId, role, KEYS.getPrivate());
    }

    /** A token signed with {@code signingKey} under the test key id. */
    public static String token(long userId, String role, PrivateKey signingKey) {
        return Jwts.builder()
                .header().keyId(KID).and()
                .issuer("nambede-auth")
                .subject(String.valueOf(userId))
                .claim("email", "user" + userId + "@nambede.local")
                .claim("role", role)
                .expiration(Date.from(Instant.now().plusSeconds(300)))
                .signWith(signingKey, Jwts.SIG.RS256)
                .compact();
    }

    /** The service's public keys in the tests: only the test key, as if read from auth-service's JWK Set. */
    @TestConfiguration
    public static class Keys {

        @Bean
        PublicKeys publicKeys() {
            return kid -> KID.equals(kid) ? KEYS.getPublic() : null;
        }
    }
}
