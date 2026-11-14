package vn.edu.qnu.st.nambede.exercise.clients;

import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import vn.edu.qnu.st.nambede.exercise.common.ApiException;
import vn.edu.qnu.st.nambede.exercise.common.RequestIdFilter;

/** Shared setup for calls to other services: short timeouts, the request id passed on, one error for "service down". */
final class RemoteServices {

    private RemoteServices() {
    }

    static RestClient restClient(String baseUrl) {
        // HTTP/1.1: the JDK client otherwise asks for an h2c upgrade, which uvicorn (learning-service) refuses on POST
        HttpClient httpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(2)).build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(5));
        return RestClient.builder().baseUrl(baseUrl).requestFactory(requestFactory)
                .requestInterceptor((request, body, execution) -> {
                    String requestId = RequestIdFilter.current();
                    if (requestId != null) {
                        request.getHeaders().set(RequestIdFilter.HEADER, requestId);
                    }
                    return execution.execute(request, body);
                })
                .build();
    }

    static ApiException unavailable(String service) {
        return new ApiException(HttpStatus.BAD_GATEWAY, service + " is unavailable");
    }
}
