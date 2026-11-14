package vn.edu.qnu.st.nambede.exercise.web.dto;

import java.time.Instant;

/** An exercise in a list. studentCount is the number of students of the class when the caller is a lecturer or admin. */
public record ExerciseSummary(
        Long id,
        Long classId,
        String className,
        String title,
        String description,
        Instant dueAt,
        boolean overdue,
        int questionCount,
        Instant createdAt,
        Integer studentCount) {
}
