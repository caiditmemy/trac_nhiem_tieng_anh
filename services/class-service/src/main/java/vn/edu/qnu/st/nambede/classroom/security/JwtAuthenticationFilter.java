package vn.edu.qnu.st.nambede.classroom.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import vn.edu.qnu.st.nambede.classroom.common.ApiException;
import vn.edu.qnu.st.nambede.classroom.jwt.JwtService;
import vn.edu.qnu.st.nambede.classroom.jwt.KeysUnavailableException;

/**
 * Reads {@code Authorization: Bearer <jwt>}, verifies it and stores the caller in the SecurityContext.
 * Requests without a token continue anonymously and the authorization rules decide.
 * Not a @Component on purpose: it must only run inside the security filter chain.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final JsonAuthErrors errors;

    public JwtAuthenticationFilter(JwtService jwtService, JsonAuthErrors errors) {
        this.jwtService = jwtService;
        this.errors = errors;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/actuator/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            chain.doFilter(request, response);
            return;
        }

        AuthUser user;
        try {
            user = jwtService.verify(header.substring(BEARER_PREFIX.length()).trim());
        } catch (JwtException | IllegalArgumentException ex) {
            SecurityContextHolder.clearContext();
            errors.commence(request, response, new BadCredentialsException("Invalid or expired token", ex));
            return;
        } catch (KeysUnavailableException ex) {
            // 502, not 401: the token may be fine, and a 401 would sign the user out of the web and the app
            SecurityContextHolder.clearContext();
            errors.fail(request, response, new ApiException(HttpStatus.BAD_GATEWAY, ex.getMessage()));
            return;
        }

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(user, null, user.authorities()));
        SecurityContextHolder.setContext(context);
        chain.doFilter(request, response);
    }
}
