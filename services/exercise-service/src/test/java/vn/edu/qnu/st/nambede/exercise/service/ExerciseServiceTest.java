package vn.edu.qnu.st.nambede.exercise.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static vn.edu.qnu.st.nambede.exercise.service.Fixtures.OTHER_TEACHER;
import static vn.edu.qnu.st.nambede.exercise.service.Fixtures.TEACHER;
import static vn.edu.qnu.st.nambede.exercise.service.Fixtures.TOKEN;
import static vn.edu.qnu.st.nambede.exercise.service.Fixtures.exercise;
import static vn.edu.qnu.st.nambede.exercise.service.Fixtures.submission;
import static vn.edu.qnu.st.nambede.exercise.service.Fixtures.tomorrow;
import static vn.edu.qnu.st.nambede.exercise.service.Fixtures.yesterday;

import java.math.BigDecimal;
import java.time.Instant;
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
import vn.edu.qnu.st.nambede.exercise.clients.ClassClient.ClassStudent;
import vn.edu.qnu.st.nambede.exercise.clients.LearningClient;
import vn.edu.qnu.st.nambede.exercise.clients.LearningClient.BankQuestion;
import vn.edu.qnu.st.nambede.exercise.common.ApiException;
import vn.edu.qnu.st.nambede.exercise.domain.Exercise;
import vn.edu.qnu.st.nambede.exercise.domain.ExerciseQuestion;
import vn.edu.qnu.st.nambede.exercise.domain.ExerciseRepository;
import vn.edu.qnu.st.nambede.exercise.domain.Submission;
import vn.edu.qnu.st.nambede.exercise.domain.SubmissionRepository;
import vn.edu.qnu.st.nambede.exercise.web.dto.CreateExerciseRequest;
import vn.edu.qnu.st.nambede.exercise.web.dto.ExerciseResults;
import vn.edu.qnu.st.nambede.exercise.web.dto.ExerciseSummary;

@ExtendWith(MockitoExtension.class)
class ExerciseServiceTest {

    private static final ClassInfo CLASS_10 = new ClassInfo(10L, "Lập trình Web - 46K1", 3L, "Trần Thị B", 3);

    @Mock
    private ExerciseRepository exercises;
    @Mock
    private SubmissionRepository submissions;
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
    void resultsListEveryStudentOfTheClassWithTheAverage() {
        Exercise exercise = exercise(tomorrow());
        Submission done = submission(50L, exercise, 2L);
        done.choose(101L, "B");
        done.choose(102L, "C");
        done.grade(Instant.now(), Grading.scoreOutOf10(2, 3), 2);
        Submission working = submission(51L, exercise, 6L);
        when(exercises.findById(1L)).thenReturn(Optional.of(exercise));
        when(classClient.get(10L, TOKEN)).thenReturn(CLASS_10);
        when(classClient.students(10L, TOKEN)).thenReturn(List.of(
                new ClassStudent(2L, "Nguyễn Văn A", "student1@nambede.local"),
                new ClassStudent(5L, "Phạm Thị D", "student2@nambede.local"),
                new ClassStudent(6L, "Lê Văn E", "student3@nambede.local")));
        when(submissions.findByExerciseId(1L)).thenReturn(List.of(done, working));

        ExerciseResults results = service.results(1L, TEACHER, TOKEN);

        assertThat(results.students()).extracting("fullName", "status", "score")
                .containsExactly(tuple("Nguyễn Văn A", "SUBMITTED", new BigDecimal("6.67")),
                        tuple("Phạm Thị D", "NOT_STARTED", null), tuple("Lê Văn E", "IN_PROGRESS", null));
        assertThat(results.averageScore()).isEqualByComparingTo("6.67");
        assertThat(results.exercise().submittedCount()).isEqualTo(1);
        assertThat(results.questions()).extracting("correctRate").containsExactly(100, 100, 0);
    }

    @Test
    void onlyTheLecturerWhoAssignedItSeesResultsOrDeletesIt() {
        when(exercises.findById(1L)).thenReturn(Optional.of(exercise(tomorrow())));
        assertStatus(() -> service.results(1L, OTHER_TEACHER, TOKEN), HttpStatus.FORBIDDEN);
        assertStatus(() -> service.delete(1L, OTHER_TEACHER), HttpStatus.FORBIDDEN);
        service.delete(1L, Fixtures.ADMIN);
        verify(exercises).delete(any(Exercise.class));
    }
}
