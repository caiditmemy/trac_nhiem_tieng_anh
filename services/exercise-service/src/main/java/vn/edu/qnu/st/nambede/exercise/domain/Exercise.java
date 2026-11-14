package vn.edu.qnu.st.nambede.exercise.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** An exercise a lecturer assigns to one class. Its questions are copies, not links to the question bank. */
@Entity
@Table(name = "exercises")
public class Exercise {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Class id from class-service (no foreign key across services). */
    @Column(name = "class_id", nullable = false)
    private Long classId;

    /** User id from auth-service. */
    @Column(name = "teacher_id", nullable = false)
    private Long teacherId;

    @Column(nullable = false)
    private String title;

    private String description;

    @Column(name = "due_at", nullable = false)
    private Instant dueAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "exercise", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position")
    private List<ExerciseQuestion> questions = new ArrayList<>();

    protected Exercise() {
        // required by JPA
    }

    public Exercise(Long classId, Long teacherId, String title, String description, Instant dueAt) {
        this.classId = classId;
        this.teacherId = teacherId;
        this.title = title;
        this.description = description;
        this.dueAt = dueAt;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public void addQuestion(ExerciseQuestion question) {
        question.setExercise(this);
        questions.add(question);
    }

    public boolean isOverdue(Instant now) {
        return now.isAfter(dueAt);
    }

    public Long getId() {
        return id;
    }

    public Long getClassId() {
        return classId;
    }

    public Long getTeacherId() {
        return teacherId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public Instant getDueAt() {
        return dueAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public List<ExerciseQuestion> getQuestions() {
        return questions;
    }
}
