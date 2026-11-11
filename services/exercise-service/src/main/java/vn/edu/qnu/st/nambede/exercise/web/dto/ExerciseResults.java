package vn.edu.qnu.st.nambede.exercise.web.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** The lecturer's view of one exercise: every student of the class, their result, and each question's success rate. */
public record ExerciseResults(
        ExerciseSummary exercise,
        BigDecimal averageScore,
        List<StudentResult> students,
        List<QuestionView> questions) {

    public record StudentResult(
            Long studentId,
            String fullName,
            String email,
            String status,
            BigDecimal score,
            Integer correctCount,
            Instant submittedAt,
            Long submissionId) {
    }
}
