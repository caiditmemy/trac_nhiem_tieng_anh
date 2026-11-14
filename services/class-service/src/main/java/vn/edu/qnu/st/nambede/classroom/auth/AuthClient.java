package vn.edu.qnu.st.nambede.classroom.auth;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import vn.edu.qnu.st.nambede.classroom.common.RequestIdFilter;

/**
 * REST client for auth-service. Services never read another service's database:
 * names and emails of users come from auth-service's API.
 */
@Component
public class AuthClient {

    private static final Logger log = LoggerFactory.getLogger(AuthClient.class);

    private final RestClient restClient;

    public AuthClient(@Value("${app.auth-service-url}") String authServiceUrl) {
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(5));
        this.restClient = RestClient.builder().baseUrl(authServiceUrl).requestFactory(requestFactory)
                .requestInterceptor((request, body, execution) -> {
                    // auth-service logs the same request id as this service
                    String requestId = RequestIdFilter.current();
                    if (requestId != null) {
                        request.getHeaders().set(RequestIdFilter.HEADER, requestId);
                    }
                    return execution.execute(request, body);
                })
                .build();
    }

    /**
     * Looks up users by id with the caller's token (GET /users/lookup on auth-service).
     * If auth-service cannot answer, returns an empty map: the caller still gets its data, only without names.
     */
    public Map<Long, UserSummary> lookup(Collection<Long> userIds, String authorization) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        String ids = userIds.stream().distinct().map(String::valueOf).collect(Collectors.joining(","));
        try {
            UserSummary[] users = restClient.get()
                    .uri(builder -> builder.path("/users/lookup").queryParam("ids", ids).build())
                    .header(HttpHeaders.AUTHORIZATION, authorization)
                    .retrieve()
                    .body(UserSummary[].class);
            if (users == null) {
                return Map.of();
            }
            return Arrays.stream(users)
                    .collect(Collectors.toMap(UserSummary::id, Function.identity(), (first, second) -> first));
        } catch (RestClientException ex) {
            log.warn("auth-service did not answer the user lookup, continuing without names: {}", ex.getMessage());
            return Map.of();
        }
    }
}
