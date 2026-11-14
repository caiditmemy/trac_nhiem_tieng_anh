package vn.edu.qnu.st.nambede.classroom.classes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;

/** A class (lớp học). Named Classroom because "Class" clashes with java.lang.Class. */
@Entity
@Table(name = "classes")
public class Classroom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true, length = 12)
    private String code;

    private String description;

    /** User id from auth-service. No foreign key: the users table lives in another service's database. */
    @Column(name = "teacher_id", nullable = false)
    private Long teacherId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Classroom() {
        // required by JPA
    }

    public Classroom(String name, String description, String code, Long teacherId) {
        this.name = name;
        this.description = description;
        this.code = code;
        this.teacherId = teacherId;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getTeacherId() {
        return teacherId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
