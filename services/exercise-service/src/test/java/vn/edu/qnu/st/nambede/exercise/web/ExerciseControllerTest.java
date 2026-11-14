package vn.edu.qnu.st.nambede.exercise.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static vn.edu.qnu.st.nambede.exercise.TestTokens.bearer;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import vn.edu.qnu.st.nambede.exercise.TestTokens;
import vn.edu.qnu.st.nambede.exercise.config.SecurityConfig;
import vn.edu.qnu.st.nambede.exercise.jwt.JwtService;
import vn.edu.qnu.st.nambede.exercise.service.ExerciseService;
import vn.edu.qnu.st.nambede.exercise.web.dto.ExerciseSummary;

@WebMvcTest(controllers = ExerciseController.class)
@Import({SecurityConfig.class, JwtService.class, TestTokens.Keys.class})
class ExerciseControllerTest {

    private static final String BODY = """
            {"classId": 10, "title": "Bài 1", "dueAt": "2099-01-01T00:00:00Z", "questionIds": [7, 8]}""";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ExerciseService exerciseService;

    @Test
    void withoutTokenIs401() throws Exception {
        mockMvc.perform(get("/exercises")).andExpect(status().isUnauthorized());
    }

    @Test
    void onlyLecturersAssignExercises() throws Exception {
        mockMvc.perform(post("/exercises").header(HttpHeaders.AUTHORIZATION, bearer(2, "STUDENT"))
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isForbidden());
        verifyNoInteractions(exerciseService);

        when(exerciseService.create(any(), any(), anyString())).thenReturn(new ExerciseSummary(1L, 10L, "Lập trình Web",
                "Bài 1", null, Instant.parse("2099-01-01T00:00:00Z"), false, 2, Instant.now(), 3));
        mockMvc.perform(post("/exercises").header(HttpHeaders.AUTHORIZATION, bearer(3, "TEACHER"))
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.questionCount").value(2));
    }

    @Test
    void studentsCannotOpenAnExerciseWithItsAnswers() throws Exception {
        mockMvc.perform(get("/exercises/1").header(HttpHeaders.AUTHORIZATION, bearer(2, "STUDENT")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(exerciseService);
    }

    @Test
    void studentsCannotDeleteAnExercise() throws Exception {
        mockMvc.perform(delete("/exercises/1").header(HttpHeaders.AUTHORIZATION, bearer(2, "STUDENT")))
                .andExpect(status().isForbidden());
    }

    @Test
    void titleAndQuestionsAreRequired() throws Exception {
        mockMvc.perform(post("/exercises").header(HttpHeaders.AUTHORIZATION, bearer(3, "TEACHER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"classId\": 10, \"title\": \" \", \"dueAt\": \"2099-01-01T00:00:00Z\", \"questionIds\": []}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }
}
