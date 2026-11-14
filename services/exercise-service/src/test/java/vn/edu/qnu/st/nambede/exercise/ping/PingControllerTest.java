package vn.edu.qnu.st.nambede.exercise.ping;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import vn.edu.qnu.st.nambede.exercise.TestTokens;
import vn.edu.qnu.st.nambede.exercise.config.SecurityConfig;
import vn.edu.qnu.st.nambede.exercise.jwt.JwtService;

@WebMvcTest(controllers = PingController.class)
@Import({SecurityConfig.class, JwtService.class, TestTokens.Keys.class})
class PingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void pingWithoutTokenReturns401() throws Exception {
        mockMvc.perform(get("/exercises/ping"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void pingWithTokenSignedByAnotherKeyReturns401() throws Exception {
        // Same key id, other private key: only auth-service's key can sign
        String forged = TestTokens.token(2L, "ADMIN", Jwts.SIG.RS256.keyPair().build().getPrivate());
        mockMvc.perform(get("/exercises/ping").header(HttpHeaders.AUTHORIZATION, "Bearer " + forged))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void pingWithTokenReturnsTheCaller() throws Exception {
        mockMvc.perform(get("/exercises/ping").header(HttpHeaders.AUTHORIZATION, TestTokens.bearer(2L, "STUDENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.service").value("exercise-service"))
                .andExpect(jsonPath("$.userId").value(2))
                .andExpect(jsonPath("$.role").value("STUDENT"));
    }

    @Test
    void everyAnswerCarriesTheRequestId() throws Exception {
        // Kept when the gateway sends one (even on a 401), made up when it is missing
        mockMvc.perform(get("/exercises/ping").header("X-Request-ID", "gw-123"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
                        .string("X-Request-ID", "gw-123"));
        mockMvc.perform(get("/exercises/ping"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
                        .string("X-Request-ID", org.hamcrest.Matchers.matchesPattern("[0-9a-f]{32}")));
    }
}
