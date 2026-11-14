package vn.edu.qnu.st.nambede.exercise.web.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** A student's attempt. Once submitted it includes the correct answers and explanations. */
public record SubmissionView(
        Long id,
        Long exerciseId,
        String exerciseTitle,
        String exerciseDescription,
        Instant dueAt,
        String status,
        Instant startedAt,
        Instant submittedAt,
        BigDecimal score,
        Integer correctCount,
        int questionCount,
        List<QuestionView> questions) {
}
