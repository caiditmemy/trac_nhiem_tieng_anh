package vn.edu.qnu.st.nambede.classroom.classes.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record JoinClassRequest(@NotBlank @Size(max = 12) String code) {
}
