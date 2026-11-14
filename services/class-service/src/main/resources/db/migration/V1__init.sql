-- Class Service schema (class_db).
-- teacher_id and student_id are user ids owned by auth-service: no foreign key across services.
CREATE TABLE classes (
    id          BIGSERIAL    PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    code        VARCHAR(12)  NOT NULL UNIQUE,
    description TEXT,
    teacher_id  BIGINT       NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE class_students (
    class_id   BIGINT      NOT NULL REFERENCES classes (id) ON DELETE CASCADE,
    student_id BIGINT      NOT NULL,
    joined_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (class_id, student_id)
);

CREATE INDEX ix_class_students_student ON class_students (student_id);
CREATE INDEX ix_classes_teacher ON classes (teacher_id);
