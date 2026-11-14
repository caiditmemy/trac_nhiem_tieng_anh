package vn.edu.qnu.st.nambede.exercise.clients;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import vn.edu.qnu.st.nambede.exercise.common.ApiException;

/** REST client for class-service, always called with the caller's token. */
@Component
public class ClassClient {

    private static final String SERVICE = "class-service";

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ClassInfo(Long id, String name, Long teacherId, String teacherName, long studentCount) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ClassStudent(Long studentId, String fullName, String email) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Exists(boolean exists) {
    }

    private final RestClient restClient;

    public ClassClient(@Value("${app.class-service-url}") String classServiceUrl) {
        this.restClient = RemoteServices.restClient(classServiceUrl);
    }

    /** Classes the caller can see: lecturer = the classes they teach, student = joined, admin = all. */
    public List<ClassInfo> visibleClasses(String authorization) {
        try {
            ClassInfo[] classes = restClient.get().uri("/classes").header(HttpHeaders.AUTHORIZATION, authorization)
                    .retrieve().body(ClassInfo[].class);
            return classes == null ? List.of() : Arrays.asList(classes);
        } catch (RestClientException ex) {
            throw RemoteServices.unavailable(SERVICE);
        }
    }

    /** One class; class-service answers 403 unless the caller teaches it, is in it, or is an admin. */
    public ClassInfo get(Long classId, String authorization) {
        try {
            return restClient.get().uri("/classes/{id}", classId).header(HttpHeaders.AUTHORIZATION, authorization)
                    .retrieve().body(ClassInfo.class);
        } catch (HttpClientErrorException.NotFound ex) {
            throw ApiException.notFound("Class " + classId + " not found");
        } catch (HttpClientErrorException.Forbidden ex) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You are not in class " + classId);
        } catch (RestClientException ex) {
            throw RemoteServices.unavailable(SERVICE);
        }
    }

    public boolean isMember(Long classId, Long studentId, String authorization) {
        try {
            Exists answer = restClient.get().uri("/classes/{id}/students/{studentId}/exists", classId, studentId)
                    .header(HttpHeaders.AUTHORIZATION, authorization).retrieve().body(Exists.class);
            return answer != null && answer.exists();
        } catch (HttpClientErrorException.NotFound | HttpClientErrorException.Forbidden ex) {
            return false;
        } catch (RestClientException ex) {
            throw RemoteServices.unavailable(SERVICE);
        }
    }

    /** Students of a class with their names (class-service reads the names from auth-service). */
    public List<ClassStudent> students(Long classId, String authorization) {
        try {
            ClassStudent[] students = restClient.get().uri("/classes/{id}/students", classId)
                    .header(HttpHeaders.AUTHORIZATION, authorization).retrieve().body(ClassStudent[].class);
            return students == null ? List.of() : Arrays.asList(students);
        } catch (HttpClientErrorException.NotFound ex) {
            return List.of();
        } catch (RestClientException ex) {
            throw RemoteServices.unavailable(SERVICE);
        }
    }
}
