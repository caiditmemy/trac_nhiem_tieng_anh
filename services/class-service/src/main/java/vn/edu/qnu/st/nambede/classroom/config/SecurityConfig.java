package vn.edu.qnu.st.nambede.classroom.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;
import vn.edu.qnu.st.nambede.classroom.jwt.JwtProperties;
import vn.edu.qnu.st.nambede.classroom.jwt.JwtService;
import vn.edu.qnu.st.nambede.classroom.security.JsonAuthErrors;
import vn.edu.qnu.st.nambede.classroom.security.JwtAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, JwtService jwtService,
            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver) throws Exception {
        JsonAuthErrors errors = new JsonAuthErrors(resolver);
        http
                .csrf(csrf -> csrf.disable())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .logout(logout -> logout.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(handling -> handling.authenticationEntryPoint(errors).accessDeniedHandler(errors))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/actuator/health/**", "/error").permitAll()
                        // Swagger: the API description is public, calling the API still needs a token
                        .requestMatchers("/v3/api-docs", "/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/classes").hasRole("TEACHER")
                        .requestMatchers(HttpMethod.POST, "/classes/join", "/classes/*/leave").hasRole("STUDENT")
                        // Everything else: any signed-in user; ClassService checks who owns the class
                        .anyRequest().authenticated())
                .addFilterBefore(new JwtAuthenticationFilter(jwtService, errors),
                        UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
