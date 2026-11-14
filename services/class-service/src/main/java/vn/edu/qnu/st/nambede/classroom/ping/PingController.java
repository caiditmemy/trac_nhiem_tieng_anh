package vn.edu.qnu.st.nambede.classroom.ping;

import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.qnu.st.nambede.classroom.security.AuthUser;

/** Phase 1 check that the service is reachable through the gateway and accepts the shared JWT. */
@Hidden // the Phase 1 connectivity check, not part of the API
@RestController
public class PingController {

    public record PingResponse(String service, Long userId, String role) {
    }

    @GetMapping("/classes/ping")
    public PingResponse ping(@AuthenticationPrincipal AuthUser caller) {
        return new PingResponse("class-service", caller.id(), caller.role());
    }
}
