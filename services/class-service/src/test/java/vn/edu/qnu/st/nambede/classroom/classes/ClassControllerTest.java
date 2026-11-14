package vn.edu.qnu.st.nambede.classroom.classes;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static vn.edu.qnu.st.nambede.classroom.TestTokens.bearer;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import vn.edu.qnu.st.nambede.classroom.TestTokens;
import vn.edu.qnu.st.nambede.classroom.classes.dto.ClassRequest;
import vn.edu.qnu.st.nambede.classroom.classes.dto.ClassResponse;
import vn.edu.qnu.st.nambede.classroom.config.SecurityConfig;
import vn.edu.qnu.st.nambede.classroom.jwt.JwtService;

@WebMvcTest(controllers = ClassController.class)
@Import({SecurityConfig.class, JwtService.class, TestTokens.Keys.class})
class ClassControllerTest {

    private static final ClassResponse CLASS_10A1 = new ClassResponse(1L, "Tiếng Anh 10A1", "K7M2QX", null, 3L,
            "Trần Thị B", 0, Instant.parse("2026-10-01T00:00:00Z"));

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ClassService classService;

    @Test
    void listWithoutTokenReturns401() throws Exception {
        mockMvc.perform(get("/classes")).andExpect(status().isUnauthorized());
        verifyNoInteractions(classService);
    }

    @Test
    void studentCannotCreateAClass() throws Exception {
        mockMvc.perform(post("/classes").header(HttpHeaders.AUTHORIZATION, bearer(2, "STUDENT"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"10A1\"}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(classService);
    }

    @Test
    void teacherCreatesAClass() throws Exception {
        when(classService.create(eq(new ClassRequest("Tiếng Anh 10A1", null)), any(), anyString()))
                .thenReturn(CLASS_10A1);

        mockMvc.perform(post("/classes").header(HttpHeaders.AUTHORIZATION, bearer(3, "TEACHER"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Tiếng Anh 10A1\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("K7M2QX"))
                .andExpect(jsonPath("$.teacherName").value("Trần Thị B"));
    }

    @Test
    void blankNameIsRejected() throws Exception {
        mockMvc.perform(post("/classes").header(HttpHeaders.AUTHORIZATION, bearer(3, "TEACHER"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\" \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void teacherCannotJoinAClass() throws Exception {
        mockMvc.perform(post("/classes/join").header(HttpHeaders.AUTHORIZATION, bearer(3, "TEACHER"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"code\":\"K7M2QX\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void studentJoinsWithACode() throws Exception {
        when(classService.join(eq("K7M2QX"), any(), anyString())).thenReturn(CLASS_10A1);

        mockMvc.perform(post("/classes/join").header(HttpHeaders.AUTHORIZATION, bearer(2, "STUDENT"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"code\":\"K7M2QX\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Tiếng Anh 10A1"));
    }
}
