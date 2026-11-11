package vn.edu.qnu.st.nambede.exercise.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.LocatorAdapter;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import java.security.Key;
import org.springframework.stereotype.Component;
import vn.edu.qnu.st.nambede.exercise.security.AuthUser;

/**
 * Verifies RS256 tokens issued by auth-service, with its public keys (JWK Set). This service can check a token but
 * cannot make one: it never holds a signing key.
 */
@Component
public class JwtService {

    private final JwtProperties properties;
    private final PublicKeys publicKeys;

    public JwtService(JwtProperties properties, PublicKeys publicKeys) {
        this.properties = properties;
        this.publicKeys = publicKeys;
    }

    /**
     * Returns the caller described by the token.
     *
     * @throws io.jsonwebtoken.JwtException if the token is malformed, expired or not signed by auth-service
     * @throws KeysUnavailableException if auth-service's public keys cannot be read yet
     */
    public AuthUser verify(String token) {
        Claims claims = Jwts.parser()
                .keyLocator(new LocatorAdapter<Key>() {
                    @Override
                    protected Key locate(JwsHeader header) {
                        Key key = publicKeys.find(header.getKeyId());
                        if (key == null) {
                            throw new UnsupportedJwtException("Token signed with an unknown key");
                        }
                        return key;
                    }
                })
                .requireIssuer(properties.issuer())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        String role = claims.get("role", String.class);
        if (claims.getSubject() == null || role == null) {
            throw new MalformedJwtException("Token is missing the sub or role claim");
        }
        return new AuthUser(Long.valueOf(claims.getSubject()), claims.get("email", String.class), role);
    }
}
