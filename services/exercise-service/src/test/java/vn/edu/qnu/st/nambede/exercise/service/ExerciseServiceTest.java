package vn.edu.qnu.st.nambede.exercise.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static vn.edu.qnu.st.nambede.exercise.service.Fixtures.OTHER_TEACHER;
import static vn.edu.qnu.st.nambede.exercise.service.Fixtures.STUDENT;
import static vn.edu.qnu.st.nambede.exercise.service.Fixtures.TEACHER;
import static vn.edu.qnu.st.nambede.exercise.service.Fixtures.TOKEN;
import static vn.edu.qnu.st.nambede.exercise.service.Fixtures.exercise;
import static vn.edu.qnu.st.nambede.exercise.service.Fixtures.tomorrow;
import static vn.edu.qnu.st.nambede.exercise.service.Fixtures.yesterday;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import vn.edu.qnu.st.nambede.exercise.clients.ClassClient;
import vn.edu.qnu.st.nambede.exercise.clients.ClassClient.ClassInfo;
import vn.edu.qnu.st.nambede.exercise.clients.LearningClient;
import vn.edu.qnu.st.nambede.exercise.clients.LearningClient.BankQuestion;
import vn.edu.qnu.st.nambede.exercise.common.ApiException;
import vn.edu.qnu.st.nambede.exercise.domain.Exercise;
import vn.edu.qnu.st.nambede.exercise.domain.ExerciseQuestion;
import vn.edu.qnu.st.nambede.exercise.domain.ExerciseRepository;
import vn.edu.qnu.st.nambede.exercise.web.dto.CreateExerciseRequest;
import vn.edu.qnu.st.nambede.exercise.web.dto.ExerciseDetail;
import vn.edu.qnu.st.nambede.exercise.web.dto.ExerciseSummary;

@ExtendWith(MockitoExtension.class)
class ExerciseServiceTest {

    private static final ClassInfo CLASS_10 = new ClassInfo(10L, "Lập trình Web - 46K1", 3L, "Trần Thị B", 3);

    @Mock
    private ExerciseRepository exercises;
    @Mock
    private ClassClient classClient;
    @Mock
    private LearningClient learningClient;
    @InjectMocks
    private ExerciseService service;

    private static void assertStatus(Runnable action, HttpStatus status) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(ApiException.class,
                ex -> assertThat(ex.getStatus()).isEqualTo(status));
    }

    @Test
    void assigningCopiesEachChosenQuestionOnce() {
        when(classClient.get(10L, TOKEN)).thenReturn(CLASS_10);
        when(learningClient.findQuestion(7L, TOKEN)).thenReturn(Optional.of(
                new BankQuestion(7L, "She ___ to school.", "go", "goes", "gone", "going", "B", "Ngôi thứ ba số ít")));
        when(learningClient.findQuestion(8L, TOKEN)).thenReturn(Optional.of(
                new BankQuestion(8L, "We ___ to Hue last year.", "go", "goes", "went", "gone", "C", null)));
        when(exercises.save(any(Exercise.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ExerciseSummary created = service.create(
                new CreateExerciseRequest(10L, " Bài 1 ", " ", tomorrow(), List.of(7L, 8L, 7L)), TEACHER, TOKEN);

        ArgumentCaptor<Exercise> saved = ArgumentCaptor.forClass(Exercise.class);
        verify(exercises).save(saved.capture());
        assertThat(saved.getValue().getTitle()).isEqualTo("Bài 1");
        assertThat(saved.getValue().getDescription()).isNull();
        assertThat(saved.getValue().getQuestions())
                .extracting(ExerciseQuestion::getSourceQuestionId, ExerciseQuestion::getPosition,
                        ExerciseQuestion::getContent, ExerciseQuestion::getCorrectOption)
                .containsExactly(tuple(7L, 1, "She ___ to school.", "B"), tuple(8L, 2, "We ___ to Hue last year.", "C"));
        assertThat(created.questionCount()).isEqualTo(2);
        assertThat(created.className()).isEqualTo("Lập trình Web - 46K1");
    }

    @Test
    void assigningChecksTheDueDateTheClassAndTheQuestions() {
        assertStatus(() -> service.create(new CreateExerciseRequest(10L, "Bài", null, yesterday(), List.of(7L)), TEACHER, TOKEN),
                HttpStatus.BAD_REQUEST);
        verify(classClient, never()).get(any(), any());

        when(classClient.get(10L, TOKEN)).thenReturn(CLASS_10);
        assertStatus(() -> service.create(new CreateExerciseRequest(10L, "Bài", null, tomorrow(), List.of(7L)), OTHER_TEACHER, TOKEN),
                HttpStatus.FORBIDDEN);

        when(learningClient.findQuestion(7L, TOKEN)).thenReturn(Optional.empty());
        assertStatus(() -> service.create(new CreateExerciseRequest(10L, "Bài", null, tomorrow(), List.of(7L)), TEACHER, TOKEN),
                HttpStatus.BAD_REQUEST);
        verify(exercises, never()).save(any());
    }

    @Test
    void detailShowsTheCopiedQuestionsWithTheirCorrectAnswers() {
        when(exercises.findById(1L)).thenReturn(Optional.of(exercise(tomorrow())));
        when(classClient.get(10L, TOKEN)).thenReturn(CLASS_10);

        ExerciseDetail detail = service.get(1L, TEACHER, TOKEN);

        assertThat(detail.exercise().title()).isEqualTo("Bài 1");
        assertThat(detail.exercise().studentCount()).isEqualTo(3);
        assertThat(detail.questions()).extracting("position", "correctOption")
                .containsExactly(tuple(1, "B"), tuple(2, "C"), tuple(3, "A"));
    }

    @Test
    void listForAStudentShowsOnlyTheirClassesAndNoStudentCount() {
        Exercise exercise = exercise(tomorrow());
        when(classClient.visibleClasses(TOKEN)).thenReturn(List.of(CLASS_10));
        when(exercises.findByClassIdInOrderByDueAtDesc(List.of(10L))).thenReturn(List.of(exercise));
        when(exercises.countQuestions(List.of(1L))).thenReturn(Collections.singletonList(new Object[] {1L, 3L}));

        List<ExerciseSummary> asStudent = service.list(10L, STUDENT, TOKEN);
        assertThat(asStudent).extracting("title", "questionCount", "studentCount").containsExactly(tuple("Bài 1", 3, null));
        List<ExerciseSummary> asTeacher = service.list(10L, TEACHER, TOKEN);
        assertThat(asTeacher).extracting("studentCount").containsExactly(3);

        assertStatus(() -> service.list(99L, STUDENT, TOKEN), HttpStatus.FORBIDDEN);
    }

    @Test
    void onlyTheLecturerWhoAssignedItOpensOrDeletesIt() {
        when(exercises.findById(1L)).thenReturn(Optional.of(exercise(tomorrow())));
        assertStatus(() -> service.get(1L, OTHER_TEACHER, TOKEN), HttpStatus.FORBIDDEN);
        assertStatus(() -> service.get(1L, STUDENT, TOKEN), HttpStatus.FORBIDDEN);
        assertStatus(() -> service.delete(1L, OTHER_TEACHER), HttpStatus.FORBIDDEN);
        service.delete(1L, Fixtures.ADMIN);
        verify(exercises).delete(any(Exercise.class));
    }
}
