package vn.edu.qnu.st.nambede.exercise.jwt;

import io.jsonwebtoken.security.Jwk;
import io.jsonwebtoken.security.JwkSet;
import io.jsonwebtoken.security.Jwks;
import io.jsonwebtoken.security.PublicJwk;
import java.net.http.HttpClient;
import java.security.PublicKey;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * The public keys of auth-service, read from its JWK Set (GET /.well-known/jwks.json) and kept in memory, so checking
 * a token needs no call at all. A key id not seen before (auth-service made a new key) reads the set again, at most
 * every {@link #MIN_REFRESH}, so made-up key ids cannot flood auth-service.
 */
@Component
public class JwksPublicKeys implements PublicKeys {

    static final Duration MIN_REFRESH = Duration.ofSeconds(10);
    private static final Logger log = LoggerFactory.getLogger(JwksPublicKeys.class);

    private final String url;
    private final RestClient restClient;
    private volatile Map<String, PublicKey> keys = Map.of();
    private volatile Instant fetchedAt = Instant.EPOCH;

    @Autowired
    public JwksPublicKeys(JwtProperties properties) {
        this(properties.jwksUrl(), RestClient.builder().requestFactory(timeouts()).build());
    }

    JwksPublicKeys(String url, RestClient restClient) {
        this.url = url;
        this.restClient = restClient;
    }

    private static JdkClientHttpRequestFactory timeouts() {
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build());
        factory.setReadTimeout(Duration.ofSeconds(5));
        return factory;
    }

    @Override
    public PublicKey find(String kid) {
        PublicKey key = keys.get(kid);
        if (key == null && Instant.now().isAfter(fetchedAt.plus(MIN_REFRESH))) {
            refresh();
            key = keys.get(kid);
        }
        return key;
    }

    private synchronized void refresh() {
        if (!Instant.now().isAfter(fetchedAt.plus(MIN_REFRESH))) {
            return; // another request just did it
        }
        fetchedAt = Instant.now();
        try {
            JwkSet set = Jwks.setParser().build().parse(restClient.get().uri(url).retrieve().body(String.class));
            Map<String, PublicKey> read = new HashMap<>();
            for (Jwk<?> jwk : set.getKeys()) {
                if (jwk instanceof PublicJwk<?> publicJwk && jwk.getId() != null) {
                    read.put(jwk.getId(), publicJwk.toKey());
                }
            }
            keys = Map.copyOf(read);
            log.info("Read {} public key(s) from {}", read.size(), url);
        } catch (RuntimeException ex) {
            if (keys.isEmpty()) {
                fetchedAt = Instant.EPOCH; // nothing to work with: try again on the next request
                throw new KeysUnavailableException("auth-service is unavailable", ex);
            }
            log.warn("Could not refresh the public keys from {}, keeping the {} known: {}", url, keys.size(), ex.getMessage());
        }
    }
}
