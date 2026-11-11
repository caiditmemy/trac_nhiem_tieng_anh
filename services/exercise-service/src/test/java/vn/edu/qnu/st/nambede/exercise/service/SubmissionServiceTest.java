package vn.edu.qnu.st.nambede.exercise.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static vn.edu.qnu.st.nambede.exercise.service.Fixtures.OTHER_STUDENT;
import static vn.edu.qnu.st.nambede.exercise.service.Fixtures.STUDENT;
import static vn.edu.qnu.st.nambede.exercise.service.Fixtures.TEACHER;
import static vn.edu.qnu.st.nambede.exercise.service.Fixtures.TOKEN;
import static vn.edu.qnu.st.nambede.exercise.service.Fixtures.exercise;
import static vn.edu.qnu.st.nambede.exercise.service.Fixtures.submission;
import static vn.edu.qnu.st.nambede.exercise.service.Fixtures.tomorrow;
import static vn.edu.qnu.st.nambede.exercise.service.Fixtures.yesterday;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import vn.edu.qnu.st.nambede.exercise.clients.ClassClient;
import vn.edu.qnu.st.nambede.exercise.common.ApiException;
import vn.edu.qnu.st.nambede.exercise.domain.Exercise;
import vn.edu.qnu.st.nambede.exercise.domain.ExerciseRepository;
import vn.edu.qnu.st.nambede.exercise.domain.Submission;
import vn.edu.qnu.st.nambede.exercise.domain.SubmissionRepository;
import vn.edu.qnu.st.nambede.exercise.web.dto.SubmissionView;

@ExtendWith(MockitoExtension.class)
class SubmissionServiceTest {

    @Mock
    private ExerciseRepository exercises;
    @Mock
    private SubmissionRepository submissions;
    @Mock
    private ClassClient classClient;
    @Mock
    private ApplicationEventPublisher events;
    @InjectMocks
    private SubmissionService service;

    private static void assertStatus(Runnable action, HttpStatus status) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(ApiException.class,
                ex -> assertThat(ex.getStatus()).isEqualTo(status));
    }

    @Test
    void startCreatesTheAttemptAndHidesTheAnswers() {
        Exercise exercise = exercise(tomorrow());
        when(exercises.findById(1L)).thenReturn(Optional.of(exercise));
        when(submissions.findByExerciseIdAndStudentId(1L, 2L)).thenReturn(Optional.empty());
        when(classClient.isMember(10L, 2L, TOKEN)).thenReturn(true);
        when(submissions.save(any(Submission.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SubmissionView view = service.start(1L, STUDENT, TOKEN);

        assertThat(view.status()).isEqualTo("IN_PROGRESS");
        assertThat(view.questions()).hasSize(3).allSatisfy(question -> {
            assertThat(question.correctOption()).isNull();
            assertThat(question.explanation()).isNull();
        });
    }

    @Test
    void startReturnsTheExistingAttemptInsteadOfASecondOne() {
        Exercise exercise = exercise(tomorrow());
        when(exercises.findById(1L)).thenReturn(Optional.of(exercise));
        when(submissions.findByExerciseIdAndStudentId(1L, 2L)).thenReturn(Optional.of(submission(50L, exercise, 2L)));
        when(classClient.isMember(10L, 2L, TOKEN)).thenReturn(true);

        assertThat(service.start(1L, STUDENT, TOKEN).id()).isEqualTo(50L);
        verify(submissions, never()).save(any());
    }

    @Test
    void startIsRefusedOutsideTheClassAndAfterTheDueDate() {
        when(submissions.findByExerciseIdAndStudentId(1L, 2L)).thenReturn(Optional.empty());
        when(exercises.findById(1L)).thenReturn(Optional.of(exercise(tomorrow())));
        when(classClient.isMember(10L, 2L, TOKEN)).thenReturn(false);
        assertStatus(() -> service.start(1L, STUDENT, TOKEN), HttpStatus.FORBIDDEN);

        when(exercises.findById(1L)).thenReturn(Optional.of(exercise(yesterday())));
        when(classClient.isMember(10L, 2L, TOKEN)).thenReturn(true);
        assertStatus(() -> service.start(1L, STUDENT, TOKEN), HttpStatus.CONFLICT);
    }

    @Test
    void submitScoresOutOf10AndRevealsAnswersAndExplanations() {
        when(classClient.isMember(10L, 2L, TOKEN)).thenReturn(true);
        Submission submission = submission(50L, exercise(tomorrow()), 2L);
        when(submissions.findByIdForUpdate(50L)).thenReturn(Optional.of(submission));
        service.answer(50L, 101L, "B", STUDENT, TOKEN); // right
        service.answer(50L, 102L, "A", STUDENT, TOKEN); // wrong; question 103 left blank

        SubmissionView result = service.submit(50L, STUDENT, TOKEN);

        assertThat(result.status()).isEqualTo("SUBMITTED");
        assertThat(result.correctCount()).isEqualTo(1);
        assertThat(result.score()).isEqualByComparingTo("3.33");
        assertThat(result.questions()).extracting("chosenOption", "correctOption", "correct")
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("B", "B", true),
                        org.assertj.core.groups.Tuple.tuple("A", "C", false),
                        org.assertj.core.groups.Tuple.tuple(null, "A", false));
        assertThat(result.questions().get(2).explanation()).isEqualTo("Giải thích câu 3");
        // After the commit, ActivityReporter turns this event into the call that adds points and a streak day
        ArgumentCaptor<ExerciseCompleted> event = ArgumentCaptor.forClass(ExerciseCompleted.class);
        verify(events).publishEvent(event.capture());
        assertThat(event.getValue()).isEqualTo(new ExerciseCompleted(50L, TOKEN));
    }

    @Test
    void anExerciseCanBeSubmittedOnlyOnce() {
        when(classClient.isMember(10L, 2L, TOKEN)).thenReturn(true);
        Submission submission = submission(50L, exercise(tomorrow()), 2L);
        when(submissions.findByIdForUpdate(50L)).thenReturn(Optional.of(submission));
        service.submit(50L, STUDENT, TOKEN);

        assertStatus(() -> service.answer(50L, 101L, "B", STUDENT, TOKEN), HttpStatus.CONFLICT);
        assertStatus(() -> service.submit(50L, STUDENT, TOKEN), HttpStatus.CONFLICT);
        verify(events, times(1)).publishEvent(any(ExerciseCompleted.class));
    }

    @Test
    void answersAreRefusedAfterTheDueDateAndOnOtherStudentsAttempts() {
        when(submissions.findByIdForUpdate(50L)).thenReturn(Optional.of(submission(50L, exercise(yesterday()), 2L)));
        assertStatus(() -> service.answer(50L, 101L, "B", STUDENT, TOKEN), HttpStatus.CONFLICT);
        assertStatus(() -> service.answer(50L, 101L, "B", OTHER_STUDENT, TOKEN), HttpStatus.FORBIDDEN);
    }

    @Test
    void answerMustBelongToTheExercise() {
        when(classClient.isMember(10L, 2L, TOKEN)).thenReturn(true);
        when(submissions.findByIdForUpdate(50L)).thenReturn(Optional.of(submission(50L, exercise(tomorrow()), 2L)));
        assertStatus(() -> service.answer(50L, 999L, "B", STUDENT, TOKEN), HttpStatus.BAD_REQUEST);
    }

    @Test
    void theLecturerSeesTheAnswersOfAnAttemptInProgress() {
        Submission submission = submission(50L, exercise(tomorrow()), 2L);
        when(submissions.findById(50L)).thenReturn(Optional.of(submission));

        assertThat(service.get(50L, TEACHER).questions().get(0).correctOption()).isEqualTo("B");
        assertThat(service.get(50L, STUDENT).questions().get(0).correctOption()).isNull();
        assertStatus(() -> service.get(50L, OTHER_STUDENT), HttpStatus.FORBIDDEN);
    }

    @Test
    void aRemovedStudentCannotContinueAnUnfinishedAttempt() {
        Exercise exercise = exercise(tomorrow());
        Submission submission = submission(50L, exercise, 2L);
        when(exercises.findById(1L)).thenReturn(Optional.of(exercise));
        when(submissions.findByExerciseIdAndStudentId(1L, 2L)).thenReturn(Optional.of(submission));
        when(submissions.findByIdForUpdate(50L)).thenReturn(Optional.of(submission));
        when(classClient.isMember(10L, 2L, TOKEN)).thenReturn(false);

        assertStatus(() -> service.start(1L, STUDENT, TOKEN), HttpStatus.FORBIDDEN);
        assertStatus(() -> service.answer(50L, 101L, "B", STUDENT, TOKEN), HttpStatus.FORBIDDEN);
        assertStatus(() -> service.submit(50L, STUDENT, TOKEN), HttpStatus.FORBIDDEN);
        verify(events, never()).publishEvent(any(ExerciseCompleted.class));
    }

    @Test
    void aRemovedStudentCanStillReadAFinishedAttempt() {
        Exercise exercise = exercise(tomorrow());
        Submission submission = submission(50L, exercise, 2L);
        submission.grade(java.time.Instant.now(), java.math.BigDecimal.TEN, 3);
        when(exercises.findById(1L)).thenReturn(Optional.of(exercise));
        when(submissions.findByExerciseIdAndStudentId(1L, 2L)).thenReturn(Optional.of(submission));

        assertThat(service.start(1L, STUDENT, TOKEN).status()).isEqualTo("SUBMITTED");
        verify(classClient, never()).isMember(anyLong(), anyLong(), anyString());
    }
}
