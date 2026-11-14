package vn.edu.qnu.st.nambede.exercise.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * A copy of a question-bank question, taken when the exercise is assigned.
 * Editing or deleting the original in learning-service later does not change this copy, so past scores stay valid.
 */
@Entity
@Table(name = "exercise_questions")
public class ExerciseQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exercise_id")
    private Exercise exercise;

    /** Id of the original question in learning-service, kept for reference only. */
    @Column(name = "source_question_id", nullable = false)
    private Long sourceQuestionId;

    @Column(nullable = false)
    private int position;

    @Column(nullable = false)
    private String content;

    @Column(name = "option_a", nullable = false)
    private String optionA;

    @Column(name = "option_b", nullable = false)
    private String optionB;

    @Column(name = "option_c", nullable = false)
    private String optionC;

    @Column(name = "option_d", nullable = false)
    private String optionD;

    @Column(name = "correct_option", nullable = false, length = 1)
    private String correctOption;

    private String explanation;

    protected ExerciseQuestion() {
        // required by JPA
    }

    public ExerciseQuestion(Long sourceQuestionId, int position, String content, String optionA, String optionB,
            String optionC, String optionD, String correctOption, String explanation) {
        this.sourceQuestionId = sourceQuestionId;
        this.position = position;
        this.content = content;
        this.optionA = optionA;
        this.optionB = optionB;
        this.optionC = optionC;
        this.optionD = optionD;
        this.correctOption = correctOption;
        this.explanation = explanation;
    }

    void setExercise(Exercise exercise) {
        this.exercise = exercise;
    }

    public Long getId() {
        return id;
    }

    public Exercise getExercise() {
        return exercise;
    }

    public Long getSourceQuestionId() {
        return sourceQuestionId;
    }

    public int getPosition() {
        return position;
    }

    public String getContent() {
        return content;
    }

    public String getOptionA() {
        return optionA;
    }

    public String getOptionB() {
        return optionB;
    }

    public String getOptionC() {
        return optionC;
    }

    public String getOptionD() {
        return optionD;
    }

    public String getCorrectOption() {
        return correctOption;
    }

    public String getExplanation() {
        return explanation;
    }
}
