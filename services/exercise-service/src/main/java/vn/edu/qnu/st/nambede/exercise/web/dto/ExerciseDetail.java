package vn.edu.qnu.st.nambede.exercise.web.dto;

import java.util.List;

/** One exercise with the questions that were copied into it. */
public record ExerciseDetail(ExerciseSummary exercise, List<QuestionView> questions) {
}
