package vn.edu.qnu.st.nambede.classroom.auth;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** The part of auth-service's user that this service needs. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record UserSummary(Long id, String fullName, String email) {
}
