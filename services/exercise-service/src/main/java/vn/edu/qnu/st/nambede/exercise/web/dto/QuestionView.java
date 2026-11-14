package vn.edu.qnu.st.nambede.exercise.web.dto;

/**
 * A question of an exercise. correctOption, explanation and correct are null while a student is still working;
 * correctRate (percent of submitted students who got it right) is only filled in the lecturer's results.
 */
public record QuestionView(
        Long id,
        int position,
        String content,
        String optionA,
        String optionB,
        String optionC,
        String optionD,
        String chosenOption,
        String correctOption,
        String explanation,
        Boolean correct,
        Integer correctRate) {
}
