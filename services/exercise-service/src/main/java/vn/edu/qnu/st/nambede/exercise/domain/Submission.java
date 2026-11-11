package vn.edu.qnu.st.nambede.exercise.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** One student's attempt at one exercise (one attempt only: unique exercise_id + student_id). */
@Entity
@Table(name = "submissions")
public class Submission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exercise_id")
    private Exercise exercise;

    /** User id from auth-service. */
    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "started_at", nullable = false, updatable = false)
    private Instant startedAt;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(precision = 5, scale = 2)
    private BigDecimal score;

    @Column(name = "correct_count")
    private Integer correctCount;

    @OneToMany(mappedBy = "submission", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SubmissionAnswer> answers = new ArrayList<>();

    protected Submission() {
        // required by JPA
    }

    public Submission(Exercise exercise, Long studentId, Instant startedAt) {
        this.exercise = exercise;
        this.studentId = studentId;
        this.startedAt = startedAt;
    }

    public boolean isSubmitted() {
        return submittedAt != null;
    }

    public Optional<SubmissionAnswer> answerFor(Long exerciseQuestionId) {
        return answers.stream().filter(answer -> answer.getExerciseQuestionId().equals(exerciseQuestionId)).findFirst();
    }

    /** Saves or replaces the chosen letter for one question. */
    public void choose(Long exerciseQuestionId, String option) {
        answerFor(exerciseQuestionId).ifPresentOrElse(
                answer -> answer.setChosenOption(option),
                () -> answers.add(new SubmissionAnswer(this, exerciseQuestionId, option)));
    }

    /** Marks every question right or wrong (unanswered counts as wrong) and records the score. */
    public void grade(Instant now, BigDecimal score, int correctCount) {
        for (ExerciseQuestion question : exercise.getQuestions()) {
            choose(question.getId(), answerFor(question.getId()).map(SubmissionAnswer::getChosenOption).orElse(null));
            SubmissionAnswer answer = answerFor(question.getId()).orElseThrow();
            answer.setCorrect(question.getCorrectOption().equals(answer.getChosenOption()));
        }
        this.submittedAt = now;
        this.score = score;
        this.correctCount = correctCount;
    }

    public Long getId() {
        return id;
    }

    public Exercise getExercise() {
        return exercise;
    }

    public Long getStudentId() {
        return studentId;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public BigDecimal getScore() {
        return score;
    }

    public Integer getCorrectCount() {
        return correctCount;
    }

    public List<SubmissionAnswer> getAnswers() {
        return answers;
    }
}
