package vn.edu.qnu.st.nambede.classroom.classes.dto;

import java.time.Instant;
import vn.edu.qnu.st.nambede.classroom.auth.UserSummary;
import vn.edu.qnu.st.nambede.classroom.classes.Classroom;

/** teacherName is null when auth-service could not be reached. */
public record ClassResponse(
        Long id,
        String name,
        String code,
        String description,
        Long teacherId,
        String teacherName,
        long studentCount,
        Instant createdAt) {

    public static ClassResponse of(Classroom classroom, long studentCount, UserSummary teacher) {
        return new ClassResponse(classroom.getId(), classroom.getName(), classroom.getCode(),
                classroom.getDescription(), classroom.getTeacherId(), teacher == null ? null : teacher.fullName(),
                studentCount, classroom.getCreatedAt());
    }
}
