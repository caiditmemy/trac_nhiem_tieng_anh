package vn.edu.qnu.st.nambede.classroom.classes.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Body of POST /classes and PUT /classes/{id}. */
public record ClassRequest(@NotBlank @Size(max = 255) String name, @Size(max = 2000) String description) {
}
