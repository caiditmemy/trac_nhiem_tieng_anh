package vn.edu.qnu.st.nambede.classroom.classes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import vn.edu.qnu.st.nambede.classroom.auth.AuthClient;
import vn.edu.qnu.st.nambede.classroom.auth.UserSummary;
import vn.edu.qnu.st.nambede.classroom.classes.dto.ClassRequest;
import vn.edu.qnu.st.nambede.classroom.classes.dto.ClassResponse;
import vn.edu.qnu.st.nambede.classroom.classes.dto.ClassStudentResponse;
import vn.edu.qnu.st.nambede.classroom.common.ApiException;
import vn.edu.qnu.st.nambede.classroom.security.AuthUser;

@ExtendWith(MockitoExtension.class)
class ClassServiceTest {

    private static final String TOKEN = "Bearer test";
    private static final AuthUser TEACHER = new AuthUser(3L, "teacher1@nambede.local", "TEACHER");
    private static final AuthUser OTHER_TEACHER = new AuthUser(4L, "teacher2@nambede.local", "TEACHER");
    private static final AuthUser STUDENT = new AuthUser(2L, "student1@nambede.local", "STUDENT");
    private static final AuthUser ADMIN = new AuthUser(1L, "admin@nambede.local", "ADMIN");

    @Mock
    private ClassroomRepository classes;
    @Mock
    private ClassMemberRepository members;
    @Mock
    private ClassCodeGenerator codeGenerator;
    @Mock
    private AuthClient authClient;
    @InjectMocks
    private ClassService service;

    private static Classroom classOf(Long teacherId) {
        return new Classroom("Tiếng Anh 10A1", null, "K7M2QX", teacherId);
    }

    @Test
    void createGivesTheClassANewCodeAndTheTeacher() {
        when(codeGenerator.next()).thenReturn("TAKEN1", "FREE22");
        when(classes.existsByCode("TAKEN1")).thenReturn(true);
        when(classes.existsByCode("FREE22")).thenReturn(false);
        when(classes.save(any(Classroom.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(authClient.lookup(List.of(3L), TOKEN))
                .thenReturn(Map.of(3L, new UserSummary(3L, "Trần Thị B", "teacher1@nambede.local")));

        ClassResponse created = service.create(new ClassRequest("  Tiếng Anh 10A1 ", " "), TEACHER, TOKEN);

        assertThat(created.code()).isEqualTo("FREE22");
        assertThat(created.name()).isEqualTo("Tiếng Anh 10A1");
        assertThat(created.description()).isNull();
        assertThat(created.teacherId()).isEqualTo(3L);
        assertThat(created.teacherName()).isEqualTo("Trần Thị B");
    }

    @Test
    void joinWithAnUnknownCodeIs404() {
        when(classes.findByCode("NOPE99")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.join(" nope99 ", STUDENT, TOKEN))
                .isInstanceOfSatisfying(ApiException.class, ex -> assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void joiningTwiceIs409() {
        when(classes.findByCode("K7M2QX")).thenReturn(Optional.of(classOf(3L)));
        when(members.existsByClassIdAndStudentId(any(), any())).thenReturn(true);

        assertThatThrownBy(() -> service.join("k7m2qx", STUDENT, TOKEN))
                .isInstanceOfSatisfying(ApiException.class, ex -> assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT));
        verify(members, never()).save(any());
    }

    @Test
    void anotherTeacherCannotUpdateTheClass() {
        when(classes.findById(10L)).thenReturn(Optional.of(classOf(3L)));

        assertThatThrownBy(() -> service.update(10L, new ClassRequest("New name", null), OTHER_TEACHER, TOKEN))
                .isInstanceOfSatisfying(ApiException.class, ex -> assertThat(ex.getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void adminCanDeleteAnyClass() {
        Classroom classroom = classOf(3L);
        when(classes.findById(10L)).thenReturn(Optional.of(classroom));

        service.delete(10L, ADMIN);

        verify(classes).delete(classroom);
    }

    @Test
    void studentsKeepTheirIdsWhenAuthServiceIsDown() {
        when(classes.findById(10L)).thenReturn(Optional.of(classOf(3L)));
        when(members.findByClassIdOrderByJoinedAtAsc(10L)).thenReturn(List.of(new ClassMember(10L, 2L)));
        when(authClient.lookup(anyCollection(), anyString())).thenReturn(Map.of());

        List<ClassStudentResponse> students = service.students(10L, TEACHER, TOKEN);

        assertThat(students).singleElement().satisfies(student -> {
            assertThat(student.studentId()).isEqualTo(2L);
            assertThat(student.fullName()).isNull();
        });
    }

    @Test
    void aStudentMayOnlyCheckTheirOwnMembership() {
        when(members.existsByClassIdAndStudentId(10L, 2L)).thenReturn(true);
        when(classes.findById(10L)).thenReturn(Optional.of(classOf(3L)));

        assertThat(service.isMember(10L, 2L, STUDENT)).isTrue();
        assertThatThrownBy(() -> service.isMember(10L, 5L, STUDENT)).isInstanceOf(ApiException.class);
    }
}
