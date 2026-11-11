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

@Entity
@Table(name = "submission_answers")
public class SubmissionAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "submission_id")
    private Submission submission;

    @Column(name = "exercise_question_id", nullable = false)
    private Long exerciseQuestionId;

    /** A, B, C or D; null when the question was left blank. */
    @Column(name = "chosen_option", length = 1)
    private String chosenOption;

    /** Set when the exercise is submitted. */
    @Column(name = "is_correct")
    private Boolean correct;

    protected SubmissionAnswer() {
        // required by JPA
    }

    SubmissionAnswer(Submission submission, Long exerciseQuestionId, String chosenOption) {
        this.submission = submission;
        this.exerciseQuestionId = exerciseQuestionId;
        this.chosenOption = chosenOption;
    }

    public Long getExerciseQuestionId() {
        return exerciseQuestionId;
    }

    public String getChosenOption() {
        return chosenOption;
    }

    void setChosenOption(String chosenOption) {
        this.chosenOption = chosenOption;
    }

    public Boolean getCorrect() {
        return correct;
    }

    void setCorrect(Boolean correct) {
        this.correct = correct;
    }
}
