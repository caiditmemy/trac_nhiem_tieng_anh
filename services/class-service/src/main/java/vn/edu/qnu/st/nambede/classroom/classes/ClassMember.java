package vn.edu.qnu.st.nambede.classroom.classes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/** A student in a class (table class_students). */
@Entity
@Table(name = "class_students")
@IdClass(ClassMember.Key.class)
public class ClassMember {

    @Id
    @Column(name = "class_id")
    private Long classId;

    /** User id from auth-service. */
    @Id
    @Column(name = "student_id")
    private Long studentId;

    @Column(name = "joined_at", nullable = false, updatable = false)
    private Instant joinedAt;

    protected ClassMember() {
        // required by JPA
    }

    public ClassMember(Long classId, Long studentId) {
        this.classId = classId;
        this.studentId = studentId;
        this.joinedAt = Instant.now();
    }

    public Long getClassId() {
        return classId;
    }

    public Long getStudentId() {
        return studentId;
    }

    public Instant getJoinedAt() {
        return joinedAt;
    }

    /** Composite primary key (class_id, student_id). */
    public static class Key implements Serializable {

        private Long classId;
        private Long studentId;

        public Key() {
        }

        public Key(Long classId, Long studentId) {
            this.classId = classId;
            this.studentId = studentId;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof Key key && Objects.equals(classId, key.classId)
                    && Objects.equals(studentId, key.studentId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(classId, studentId);
        }
    }
}
