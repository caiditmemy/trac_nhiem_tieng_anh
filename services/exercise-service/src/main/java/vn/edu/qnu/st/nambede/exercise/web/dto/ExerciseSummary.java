package vn.edu.qnu.st.nambede.exercise.web.dto;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * An exercise in a list. Lecturer and admin views fill submittedCount and studentCount;
 * the student view fills myStatus (NOT_STARTED, IN_PROGRESS, SUBMITTED), myScore and mySubmissionId.
 */
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
        Integer submittedCount,
        Integer studentCount,
        String myStatus,
        BigDecimal myScore,
        Long mySubmissionId) {
}
