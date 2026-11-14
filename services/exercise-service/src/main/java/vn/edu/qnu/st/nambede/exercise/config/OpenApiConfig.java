package vn.edu.qnu.st.nambede.exercise.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * Swagger of this service alone: http://localhost:8083/swagger-ui.html. The gateway shows every service in one page,
 * calling them through /api: http://localhost:8000/docs. Every service names its scheme "bearerAuth", so a token
 * entered once in Swagger works for all of them.
 */
@Configuration
public class OpenApiConfig {

    static {
        // The Authorization header that controllers forward to other services is sent by the bearerAuth scheme
        SpringDocUtils.getConfig().addAnnotationsToIgnore(RequestHeader.class);
    }

    @Bean
    OpenAPI openApi() {
        return new OpenAPI()
                .info(new Info().title("NamBeDe Exercise Service").version("0.1.0")
                        .description("Giao bài tập cho lớp. Qua gateway: /api/exercises"))
                .components(new Components().addSecuritySchemes("bearerAuth", new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")
                        .description("accessToken nhận được từ POST /api/auth/login")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
}
