package vn.edu.qnu.st.nambede.exercise.clients;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import vn.edu.qnu.st.nambede.exercise.common.ApiException;

/** REST client for learning-service: reads questions of the shared bank to copy them into an exercise. */
@Component
public class LearningClient {

    private static final String SERVICE = "learning-service";

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record BankQuestion(Long id, String content, String optionA, String optionB, String optionC,
            String optionD, String correctOption, String explanation) {
    }

    private final RestClient restClient;

    public LearningClient(@Value("${app.learning-service-url}") String learningServiceUrl) {
        this.restClient = RemoteServices.restClient(learningServiceUrl);
    }

    public Optional<BankQuestion> findQuestion(Long questionId, String authorization) {
        try {
            return Optional.ofNullable(restClient.get().uri("/questions/{id}", questionId)
                    .header(HttpHeaders.AUTHORIZATION, authorization).retrieve().body(BankQuestion.class));
        } catch (HttpClientErrorException.NotFound ex) {
            return Optional.empty();
        } catch (HttpClientErrorException.Forbidden ex) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You cannot read the question bank");
        } catch (RestClientException ex) {
            throw RemoteServices.unavailable(SERVICE);
        }
    }
}
