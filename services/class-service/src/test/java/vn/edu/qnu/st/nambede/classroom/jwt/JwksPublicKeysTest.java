package vn.edu.qnu.st.nambede.classroom.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import io.jsonwebtoken.security.Jwks;
import io.jsonwebtoken.security.RsaPublicJwk;
import java.security.interfaces.RSAPublicKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import vn.edu.qnu.st.nambede.classroom.TestTokens;

class JwksPublicKeysTest {

    private static final String URL = "http://auth.test/.well-known/jwks.json";

    private MockRestServiceServer authService;
    private JwksPublicKeys keys;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        authService = MockRestServiceServer.bindTo(builder).build();
        keys = new JwksPublicKeys(URL, builder.build());
    }

    /** auth-service's JWK Set with the test key, as GET /.well-known/jwks.json answers it. */
    private static String jwkSet() {
        RsaPublicJwk jwk = Jwks.builder().key((RSAPublicKey) TestTokens.KEYS.getPublic()).id(TestTokens.KID).build();
        return "{\"keys\":[{\"kty\":\"RSA\",\"use\":\"sig\",\"alg\":\"RS256\",\"kid\":\"" + TestTokens.KID
                + "\",\"n\":\"" + jwk.get("n") + "\",\"e\":\"" + jwk.get("e") + "\"}]}";
    }

    @Test
    void readsTheKeysOnceAndKeepsThem() {
        authService.expect(ExpectedCount.once(), requestTo(URL)).andRespond(withSuccess(jwkSet(), MediaType.APPLICATION_JSON));

        assertThat(keys.find(TestTokens.KID)).isEqualTo(TestTokens.KEYS.getPublic());
        assertThat(keys.find(TestTokens.KID)).isEqualTo(TestTokens.KEYS.getPublic());
        // An unknown key id right after: not asked again before MIN_REFRESH
        assertThat(keys.find("someone-elses-key")).isNull();
        authService.verify();
    }

    @Test
    void authServiceDownWithNoKnownKeyIsReportedAsUnavailable() {
        authService.expect(requestTo(URL)).andRespond(withServerError());

        assertThatThrownBy(() -> keys.find(TestTokens.KID)).isInstanceOf(KeysUnavailableException.class)
                .hasMessage("auth-service is unavailable");
    }
}
