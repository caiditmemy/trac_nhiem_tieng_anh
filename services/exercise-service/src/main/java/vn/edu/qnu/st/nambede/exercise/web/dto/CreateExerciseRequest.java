package vn.edu.qnu.st.nambede.exercise.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;

public record CreateExerciseRequest(
        @NotNull Long classId,
        @NotBlank @Size(max = 255) String title,
        @Size(max = 2000) String description,
        @NotNull Instant dueAt,
        @NotEmpty @Size(max = 50) List<@NotNull Long> questionIds) {
}
