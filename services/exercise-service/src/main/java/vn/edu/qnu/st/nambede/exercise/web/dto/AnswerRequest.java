package vn.edu.qnu.st.nambede.exercise.web.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record AnswerRequest(@NotNull Long exerciseQuestionId, @NotNull @Pattern(regexp = "[ABCD]") String chosenOption) {
}
