package vn.edu.qnu.st.nambede.exercise.web.dto;

/** A question copied into an exercise, with its correct answer (lecturer and admin only). */
public record QuestionView(
        Long id,
        int position,
        String content,
        String optionA,
        String optionB,
        String optionC,
        String optionD,
        String correctOption,
        String explanation) {
}
