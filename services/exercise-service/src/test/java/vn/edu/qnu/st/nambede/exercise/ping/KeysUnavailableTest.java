package vn.edu.qnu.st.nambede.exercise.ping;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import vn.edu.qnu.st.nambede.exercise.TestTokens;
import vn.edu.qnu.st.nambede.exercise.config.SecurityConfig;
import vn.edu.qnu.st.nambede.exercise.jwt.JwtService;
import vn.edu.qnu.st.nambede.exercise.jwt.KeysUnavailableException;
import vn.edu.qnu.st.nambede.exercise.jwt.PublicKeys;

/** auth-service cannot be reached for its keys: 502, so the web and the app do not sign the user out. */
@WebMvcTest(controllers = PingController.class)
@Import({SecurityConfig.class, JwtService.class, KeysUnavailableTest.NoKeys.class})
class KeysUnavailableTest {

    @TestConfiguration
    static class NoKeys {

        @Bean
        PublicKeys publicKeys() {
            return kid -> {
                throw new KeysUnavailableException("auth-service is unavailable", null);
            };
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    void aTokenThatCannotBeCheckedYetGets502Not401() throws Exception {
        mockMvc.perform(get("/exercises/ping").header(HttpHeaders.AUTHORIZATION, TestTokens.bearer(2L, "STUDENT")))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.message").value("auth-service is unavailable"));
    }
}
