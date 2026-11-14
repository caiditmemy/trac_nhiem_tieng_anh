package vn.edu.qnu.st.nambede.exercise.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
import vn.edu.qnu.st.nambede.exercise.domain.SubmissionAnswer;
import vn.edu.qnu.st.nambede.exercise.domain.SubmissionRepository;
import vn.edu.qnu.st.nambede.exercise.security.AuthUser;
import vn.edu.qnu.st.nambede.exercise.web.dto.CreateExerciseRequest;
import vn.edu.qnu.st.nambede.exercise.web.dto.ExerciseResults;
import vn.edu.qnu.st.nambede.exercise.web.dto.ExerciseResults.StudentResult;
import vn.edu.qnu.st.nambede.exercise.web.dto.ExerciseSummary;
import vn.edu.qnu.st.nambede.exercise.web.dto.QuestionView;

/**
 * Lecturer side of exercises. {@code authorization} is the caller's "Bearer ..." header,
 * forwarded to class-service and learning-service.
 */
@Service
public class ExerciseService {

    private final ExerciseRepository exercises;
    private final SubmissionRepository submissions;
    private final ClassClient classClient;
    private final LearningClient learningClient;

    public ExerciseService(ExerciseRepository exercises, SubmissionRepository submissions, ClassClient classClient,
            LearningClient learningClient) {
        this.exercises = exercises;
        this.submissions = submissions;
        this.classClient = classClient;
        this.learningClient = learningClient;
    }

    /**
     * Assigns an exercise to a class the lecturer teaches, copying each chosen question from the bank.
     */
    @Transactional
    public ExerciseSummary create(CreateExerciseRequest request, AuthUser teacher, String authorization) {
        if (!request.dueAt().isAfter(Instant.now())) {
            throw ApiException.badRequest("The due date must be in the future");
        }
        ClassInfo classInfo = classClient.get(request.classId(), authorization);
        if (!teacher.id().equals(classInfo.teacherId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only the teacher of this class can assign exercises");
        }
        Exercise exercise = new Exercise(request.classId(), teacher.id(), request.title().trim(),
                blankToNull(request.description()), request.dueAt());
        int position = 1;
        for (Long questionId : new LinkedHashSet<>(request.questionIds())) {
            BankQuestion question = learningClient.findQuestion(questionId, authorization)
                    .orElseThrow(() -> ApiException.badRequest("Question " + questionId + " does not exist"));
            // A copy, not a link: later edits in the question bank must not change this exercise or its scores
            exercise.addQuestion(new ExerciseQuestion(question.id(), position++, question.content(), question.optionA(),
                    question.optionB(), question.optionC(), question.optionD(), question.correctOption(),
                    question.explanation()));
        }
        exercises.save(exercise);
        return staffSummary(exercise, classInfo, exercise.getQuestions().size(), 0);
    }

    /** Lecturer: exercises of the classes they teach. Student: exercises of their classes, with their status. Admin: all. */
    @Transactional(readOnly = true)
    public List<ExerciseSummary> list(Long classId, AuthUser caller, String authorization) {
        Map<Long, ClassInfo> classes = classClient.visibleClasses(authorization).stream()
                .collect(Collectors.toMap(ClassInfo::id, Function.identity(), (first, second) -> first));
        if (classId != null && !classes.containsKey(classId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You are not in class " + classId);
        }
        Collection<Long> classIds = classId != null ? List.of(classId) : classes.keySet();
        List<Exercise> found = classIds.isEmpty() ? List.of() : exercises.findByClassIdInOrderByDueAtDesc(classIds);
        if (found.isEmpty()) {
            return List.of();
        }
        List<Long> ids = found.stream().map(Exercise::getId).toList();
        Map<Long, Long> questionCounts = counts(exercises.countQuestions(ids));

        if (caller.isStudent()) {
            Map<Long, Submission> mine = submissions.findByStudentIdAndExerciseIdIn(caller.id(), ids).stream()
                    .collect(Collectors.toMap(submission -> submission.getExercise().getId(), Function.identity()));
            return found.stream()
                    .map(exercise -> studentSummary(exercise, classes.get(exercise.getClassId()),
                            questionCounts.getOrDefault(exercise.getId(), 0L).intValue(), mine.get(exercise.getId())))
                    .toList();
        }
        Map<Long, Long> submitted = counts(submissions.countSubmitted(ids));
        return found.stream()
                .map(exercise -> staffSummary(exercise, classes.get(exercise.getClassId()),
                        questionCounts.getOrDefault(exercise.getId(), 0L).intValue(),
                        submitted.getOrDefault(exercise.getId(), 0L).intValue()))
                .toList();
    }

    @Transactional(readOnly = true)
    public ExerciseSummary get(Long exerciseId, AuthUser caller, String authorization) {
        Exercise exercise = find(exerciseId);
        ClassInfo classInfo = classClient.get(exercise.getClassId(), authorization);
        if (caller.isStudent()) {
            Submission mine = submissions.findByExerciseIdAndStudentId(exerciseId, caller.id()).orElse(null);
            return studentSummary(exercise, classInfo, exercise.getQuestions().size(), mine);
        }
        ensureCanManage(exercise, caller);
        int submitted = (int) submissions.findByExerciseId(exerciseId).stream().filter(Submission::isSubmitted).count();
        return staffSummary(exercise, classInfo, exercise.getQuestions().size(), submitted);
    }

    /** Every student of the class with their result, plus how many got each question right. */
    @Transactional(readOnly = true)
    public ExerciseResults results(Long exerciseId, AuthUser caller, String authorization) {
        Exercise exercise = find(exerciseId);
        ensureCanManage(exercise, caller);
        ClassInfo classInfo = classClient.get(exercise.getClassId(), authorization);
        List<ClassStudent> students = classClient.students(exercise.getClassId(), authorization);
        List<Submission> all = submissions.findByExerciseId(exerciseId);
        List<Submission> submitted = all.stream().filter(Submission::isSubmitted).toList();

        Map<Long, Submission> byStudent = new LinkedHashMap<>();
        all.forEach(submission -> byStudent.put(submission.getStudentId(), submission));
        List<StudentResult> rows = new ArrayList<>();
        for (ClassStudent student : students) {
            rows.add(result(student.studentId(), student.fullName(), student.email(), byStudent.remove(student.studentId())));
        }
        // Students who left the class after starting keep their result
        byStudent.values().forEach(submission -> rows.add(result(submission.getStudentId(), null, null, submission)));

        BigDecimal average = submitted.isEmpty() ? null
                : submitted.stream().map(Submission::getScore).reduce(BigDecimal.ZERO, BigDecimal::add)
                        .divide(BigDecimal.valueOf(submitted.size()), 2, RoundingMode.HALF_UP);

        List<QuestionView> questions = exercise.getQuestions().stream().map(question -> {
            Integer rate = null;
            if (!submitted.isEmpty()) {
                long right = submitted.stream().filter(submission -> submission.answerFor(question.getId())
                        .map(SubmissionAnswer::getCorrect).orElse(false)).count();
                rate = (int) Math.round(right * 100.0 / submitted.size());
            }
            return new QuestionView(question.getId(), question.getPosition(), question.getContent(),
                    question.getOptionA(), question.getOptionB(), question.getOptionC(), question.getOptionD(), null,
                    question.getCorrectOption(), question.getExplanation(), null, rate);
        }).toList();

        return new ExerciseResults(staffSummary(exercise, classInfo, exercise.getQuestions().size(), submitted.size()),
                average, rows, questions);
    }

    @Transactional
    public void delete(Long exerciseId, AuthUser caller) {
        Exercise exercise = find(exerciseId);
        ensureCanManage(exercise, caller);
        exercises.delete(exercise);
    }

    Exercise find(Long exerciseId) {
        return exercises.findById(exerciseId)
                .orElseThrow(() -> ApiException.notFound("Exercise " + exerciseId + " not found"));
    }

    private static void ensureCanManage(Exercise exercise, AuthUser caller) {
        boolean owner = caller.isTeacher() && exercise.getTeacherId().equals(caller.id());
        if (!caller.isAdmin() && !owner) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only the lecturer who assigned this exercise can do this");
        }
    }

    private static StudentResult result(Long studentId, String fullName, String email, Submission submission) {
        if (submission == null) {
            return new StudentResult(studentId, fullName, email, "NOT_STARTED", null, null, null, null);
        }
        return new StudentResult(studentId, fullName, email, status(submission), submission.getScore(),
                submission.getCorrectCount(), submission.getSubmittedAt(), submission.getId());
    }

    static String status(Submission submission) {
        if (submission == null) {
            return "NOT_STARTED";
        }
        return submission.isSubmitted() ? "SUBMITTED" : "IN_PROGRESS";
    }

    private static ExerciseSummary staffSummary(Exercise exercise, ClassInfo classInfo, int questionCount, int submitted) {
        return new ExerciseSummary(exercise.getId(), exercise.getClassId(), classInfo == null ? null : classInfo.name(),
                exercise.getTitle(), exercise.getDescription(), exercise.getDueAt(), exercise.isOverdue(Instant.now()),
                questionCount, exercise.getCreatedAt(), submitted,
                classInfo == null ? null : (int) classInfo.studentCount(), null, null, null);
    }

    private static ExerciseSummary studentSummary(Exercise exercise, ClassInfo classInfo, int questionCount,
            Submission mine) {
        return new ExerciseSummary(exercise.getId(), exercise.getClassId(), classInfo == null ? null : classInfo.name(),
                exercise.getTitle(), exercise.getDescription(), exercise.getDueAt(), exercise.isOverdue(Instant.now()),
                questionCount, exercise.getCreatedAt(), null, null, status(mine),
                mine == null ? null : mine.getScore(), mine == null ? null : mine.getId());
    }

    private static Map<Long, Long> counts(List<Object[]> rows) {
        Map<Long, Long> result = new HashMap<>();
        for (Object[] row : rows) {
            result.put((Long) row[0], (Long) row[1]);
        }
        return result;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
