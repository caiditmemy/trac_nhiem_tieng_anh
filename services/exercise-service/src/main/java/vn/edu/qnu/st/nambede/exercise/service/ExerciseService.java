package vn.edu.qnu.st.nambede.exercise.service;

import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
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
import vn.edu.qnu.st.nambede.exercise.clients.LearningClient;
import vn.edu.qnu.st.nambede.exercise.clients.LearningClient.BankQuestion;
import vn.edu.qnu.st.nambede.exercise.common.ApiException;
import vn.edu.qnu.st.nambede.exercise.domain.Exercise;
import vn.edu.qnu.st.nambede.exercise.domain.ExerciseQuestion;
import vn.edu.qnu.st.nambede.exercise.domain.ExerciseRepository;
import vn.edu.qnu.st.nambede.exercise.security.AuthUser;
import vn.edu.qnu.st.nambede.exercise.web.dto.CreateExerciseRequest;
import vn.edu.qnu.st.nambede.exercise.web.dto.ExerciseDetail;
import vn.edu.qnu.st.nambede.exercise.web.dto.ExerciseSummary;
import vn.edu.qnu.st.nambede.exercise.web.dto.QuestionView;

/**
 * Assigning and listing exercises. {@code authorization} is the caller's "Bearer ..." header,
 * forwarded to class-service and learning-service.
 */
@Service
public class ExerciseService {

    private final ExerciseRepository exercises;
    private final ClassClient classClient;
    private final LearningClient learningClient;

    public ExerciseService(ExerciseRepository exercises, ClassClient classClient, LearningClient learningClient) {
        this.exercises = exercises;
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
        return summary(exercise, classInfo, exercise.getQuestions().size(), true);
    }

    /** Lecturer: exercises of the classes they teach. Student: exercises of their classes. Admin: all. */
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
        return found.stream()
                .map(exercise -> summary(exercise, classes.get(exercise.getClassId()),
                        questionCounts.getOrDefault(exercise.getId(), 0L).intValue(), !caller.isStudent()))
                .toList();
    }

    /** The exercise with its copied questions and correct answers: only for the lecturer who assigned it, or an admin. */
    @Transactional(readOnly = true)
    public ExerciseDetail get(Long exerciseId, AuthUser caller, String authorization) {
        Exercise exercise = find(exerciseId);
        ensureCanManage(exercise, caller);
        ClassInfo classInfo = classClient.get(exercise.getClassId(), authorization);
        List<QuestionView> questions = exercise.getQuestions().stream()
                .map(question -> new QuestionView(question.getId(), question.getPosition(), question.getContent(),
                        question.getOptionA(), question.getOptionB(), question.getOptionC(), question.getOptionD(),
                        question.getCorrectOption(), question.getExplanation()))
                .toList();
        return new ExerciseDetail(summary(exercise, classInfo, questions.size(), true), questions);
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

    private static ExerciseSummary summary(Exercise exercise, ClassInfo classInfo, int questionCount,
            boolean withStudentCount) {
        return new ExerciseSummary(exercise.getId(), exercise.getClassId(), classInfo == null ? null : classInfo.name(),
                exercise.getTitle(), exercise.getDescription(), exercise.getDueAt(), exercise.isOverdue(Instant.now()),
                questionCount, exercise.getCreatedAt(),
                withStudentCount && classInfo != null ? (int) classInfo.studentCount() : null);
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
