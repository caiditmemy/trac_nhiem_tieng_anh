package vn.edu.qnu.st.nambede.exercise.service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.qnu.st.nambede.exercise.clients.ClassClient;
import vn.edu.qnu.st.nambede.exercise.common.ApiException;
import vn.edu.qnu.st.nambede.exercise.domain.Exercise;
import vn.edu.qnu.st.nambede.exercise.domain.ExerciseQuestion;
import vn.edu.qnu.st.nambede.exercise.domain.ExerciseRepository;
import vn.edu.qnu.st.nambede.exercise.domain.Submission;
import vn.edu.qnu.st.nambede.exercise.domain.SubmissionAnswer;
import vn.edu.qnu.st.nambede.exercise.domain.SubmissionRepository;
import vn.edu.qnu.st.nambede.exercise.security.AuthUser;
import vn.edu.qnu.st.nambede.exercise.web.dto.QuestionView;
import vn.edu.qnu.st.nambede.exercise.web.dto.SubmissionView;

/** Student side: one attempt per exercise, answers saved as they are chosen, score out of 10 on submit. */
@Service
public class SubmissionService {

    private final ExerciseRepository exercises;
    private final SubmissionRepository submissions;
    private final ClassClient classClient;
    private final ApplicationEventPublisher events;

    public SubmissionService(ExerciseRepository exercises, SubmissionRepository submissions, ClassClient classClient,
            ApplicationEventPublisher events) {
        this.exercises = exercises;
        this.submissions = submissions;
        this.classClient = classClient;
        this.events = events;
    }

    /** Opens the student's attempt, creating it the first time. A submitted attempt is returned with its result. */
    @Transactional
    public SubmissionView start(Long exerciseId, AuthUser student, String authorization) {
        Exercise exercise = exercises.findById(exerciseId)
                .orElseThrow(() -> ApiException.notFound("Exercise " + exerciseId + " not found"));
        Optional<Submission> existing = submissions.findByExerciseIdAndStudentId(exerciseId, student.id());
        if (existing.isPresent() && existing.get().isSubmitted()) {
            return view(existing.get(), true);
        }
        // Exercise Service asks Class Service whether the student is in the class
        ensureMember(exercise, student, authorization);
        if (existing.isPresent()) {
            return view(existing.get(), false);
        }
        if (exercise.isOverdue(Instant.now())) {
            throw ApiException.conflict("The due date of this exercise has passed");
        }
        return view(submissions.save(new Submission(exercise, student.id(), Instant.now())), false);
    }

    @Transactional
    public SubmissionView answer(Long submissionId, Long exerciseQuestionId, String option, AuthUser student,
            String authorization) {
        Submission submission = findOwnForUpdate(submissionId, student);
        ensureOpen(submission);
        ensureMember(submission.getExercise(), student, authorization);
        boolean inExercise = submission.getExercise().getQuestions().stream()
                .anyMatch(question -> question.getId().equals(exerciseQuestionId));
        if (!inExercise) {
            throw ApiException.badRequest("Question " + exerciseQuestionId + " is not part of this exercise");
        }
        submission.choose(exerciseQuestionId, option);
        return view(submission, false);
    }

    /** Grades against the copied questions; the student sees the answers and explanations right away. */
    @Transactional
    public SubmissionView submit(Long submissionId, AuthUser student, String authorization) {
        Submission submission = findOwnForUpdate(submissionId, student);
        ensureOpen(submission);
        ensureMember(submission.getExercise(), student, authorization);
        List<ExerciseQuestion> questions = submission.getExercise().getQuestions();
        int correct = (int) questions.stream()
                .filter(question -> submission.answerFor(question.getId())
                        .map(answer -> question.getCorrectOption().equals(answer.getChosenOption())).orElse(false))
                .count();
        submission.grade(Instant.now(), Grading.scoreOutOf10(correct, questions.size()), correct);
        // After the commit, ActivityReporter asks learning-service to add the activity points and a streak day
        events.publishEvent(new ExerciseCompleted(submission.getId(), authorization));
        return view(submission, true);
    }

    /** The student's own attempt, or any attempt for the lecturer who assigned the exercise and for admins. */
    @Transactional(readOnly = true)
    public SubmissionView get(Long submissionId, AuthUser caller) {
        Submission submission = find(submissionId);
        boolean owner = submission.getStudentId().equals(caller.id());
        boolean staff = caller.isAdmin()
                || (caller.isTeacher() && submission.getExercise().getTeacherId().equals(caller.id()));
        if (!owner && !staff) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You cannot see this submission");
        }
        return view(submission, submission.isSubmitted() || staff);
    }

    private Submission find(Long submissionId) {
        return submissions.findById(submissionId)
                .orElseThrow(() -> ApiException.notFound("Submission " + submissionId + " not found"));
    }

    /** Locks the row so a concurrent answer cannot slip in between the status check and the grading. */
    private Submission findOwnForUpdate(Long submissionId, AuthUser student) {
        Submission submission = submissions.findByIdForUpdate(submissionId)
                .orElseThrow(() -> ApiException.notFound("Submission " + submissionId + " not found"));
        if (!submission.getStudentId().equals(student.id())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "This is not your submission");
        }
        return submission;
    }

    private void ensureMember(Exercise exercise, AuthUser student, String authorization) {
        if (!classClient.isMember(exercise.getClassId(), student.id(), authorization)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You are not in the class of this exercise");
        }
    }

    private static void ensureOpen(Submission submission) {
        if (submission.isSubmitted()) {
            throw ApiException.conflict("This exercise has already been submitted");
        }
        if (submission.getExercise().isOverdue(Instant.now())) {
            throw ApiException.conflict("The due date of this exercise has passed");
        }
    }

    /** `reveal` adds correct answers and explanations (after submitting, or for the lecturer). */
    static SubmissionView view(Submission submission, boolean reveal) {
        Exercise exercise = submission.getExercise();
        List<QuestionView> questions = exercise.getQuestions().stream().map(question -> {
            String chosen = submission.answerFor(question.getId()).map(SubmissionAnswer::getChosenOption).orElse(null);
            return new QuestionView(question.getId(), question.getPosition(), question.getContent(),
                    question.getOptionA(), question.getOptionB(), question.getOptionC(), question.getOptionD(), chosen,
                    reveal ? question.getCorrectOption() : null, reveal ? question.getExplanation() : null,
                    reveal ? question.getCorrectOption().equals(chosen) : null, null);
        }).toList();
        return new SubmissionView(submission.getId(), exercise.getId(), exercise.getTitle(), exercise.getDescription(),
                exercise.getDueAt(), ExerciseService.status(submission), submission.getStartedAt(),
                submission.getSubmittedAt(), submission.getScore(), submission.getCorrectCount(), questions.size(),
                questions);
    }
}
