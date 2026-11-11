package vn.edu.qnu.st.nambede.exercise.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.springframework.test.util.ReflectionTestUtils;
import vn.edu.qnu.st.nambede.exercise.domain.Exercise;
import vn.edu.qnu.st.nambede.exercise.domain.ExerciseQuestion;
import vn.edu.qnu.st.nambede.exercise.domain.Submission;
import vn.edu.qnu.st.nambede.exercise.security.AuthUser;

/** Entities with ids already set, as if loaded from the database. */
final class Fixtures {

    static final String TOKEN = "Bearer test";
    static final AuthUser ADMIN = new AuthUser(1L, "admin@nambede.local", "ADMIN");
    static final AuthUser STUDENT = new AuthUser(2L, "student1@nambede.local", "STUDENT");
    static final AuthUser TEACHER = new AuthUser(3L, "teacher1@nambede.local", "TEACHER");
    static final AuthUser OTHER_TEACHER = new AuthUser(4L, "teacher2@nambede.local", "TEACHER");
    static final AuthUser OTHER_STUDENT = new AuthUser(5L, "student2@nambede.local", "STUDENT");

    private Fixtures() {
    }

    static Instant tomorrow() {
        return Instant.now().plus(1, ChronoUnit.DAYS);
    }

    static Instant yesterday() {
        return Instant.now().minus(1, ChronoUnit.DAYS);
    }

    /** Exercise 1 in class 10 by lecturer 3, with three questions whose correct answers are B, C, A. */
    static Exercise exercise(Instant dueAt) {
        Exercise exercise = new Exercise(10L, 3L, "Bài 1", null, dueAt);
        ReflectionTestUtils.setField(exercise, "id", 1L);
        exercise.addQuestion(question(101L, 1, "B"));
        exercise.addQuestion(question(102L, 2, "C"));
        exercise.addQuestion(question(103L, 3, "A"));
        return exercise;
    }

    static ExerciseQuestion question(Long id, int position, String correct) {
        ExerciseQuestion question = new ExerciseQuestion(900L + position, position, "Câu " + position, "a", "b", "c",
                "d", correct, "Giải thích câu " + position);
        ReflectionTestUtils.setField(question, "id", id);
        return question;
    }

    static Submission submission(Long id, Exercise exercise, Long studentId) {
        Submission submission = new Submission(exercise, studentId, Instant.now());
        ReflectionTestUtils.setField(submission, "id", id);
        return submission;
    }
}
