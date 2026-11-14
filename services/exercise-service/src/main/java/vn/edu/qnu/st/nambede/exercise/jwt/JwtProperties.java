package vn.edu.qnu.st.nambede.exercise.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** JWT settings bound from {@code app.jwt.*} in application.yml. */
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
        @DefaultValue("nambede-auth") String issuer,
        /* Where auth-service publishes the public keys that verify its tokens (RS256) */
        @DefaultValue("http://localhost:8081/.well-known/jwks.json") String jwksUrl) {
}
