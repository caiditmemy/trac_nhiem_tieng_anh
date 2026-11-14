package vn.edu.qnu.st.nambede.classroom.classes.dto;

import java.time.Instant;

/** fullName and email come from auth-service and are null when it could not be reached. */
public record ClassStudentResponse(Long studentId, String fullName, String email, Instant joinedAt) {
}
