-- Exercise Service schema (exercise_db).
-- class_id, teacher_id, student_id and source_question_id are owned by other services: no cross-service FK.
CREATE TABLE exercises (
    id               BIGSERIAL    PRIMARY KEY,
    class_id         BIGINT       NOT NULL,
    teacher_id       BIGINT       NOT NULL,
    title            VARCHAR(255) NOT NULL,
    description      TEXT,
    start_at         TIMESTAMPTZ,
    due_at           TIMESTAMPTZ,
    duration_minutes INT,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Snapshot of each question at the moment the exercise is assigned.
-- Editing the question later in learning-service does not change past exercises or scores.
CREATE TABLE exercise_questions (
    id                 BIGSERIAL PRIMARY KEY,
    exercise_id        BIGINT    NOT NULL REFERENCES exercises (id) ON DELETE CASCADE,
    source_question_id BIGINT    NOT NULL,
    position           INT       NOT NULL,
    content            TEXT      NOT NULL,
    option_a           TEXT      NOT NULL,
    option_b           TEXT      NOT NULL,
    option_c           TEXT      NOT NULL,
    option_d           TEXT      NOT NULL,
    correct_option     CHAR(1)   NOT NULL CHECK (correct_option IN ('A', 'B', 'C', 'D')),
    explanation        TEXT,
    UNIQUE (exercise_id, position)
);

CREATE TABLE submissions (
    id           BIGSERIAL    PRIMARY KEY,
    exercise_id  BIGINT       NOT NULL REFERENCES exercises (id) ON DELETE CASCADE,
    student_id   BIGINT       NOT NULL,
    started_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    submitted_at TIMESTAMPTZ,
    score        NUMERIC(5, 2),
    UNIQUE (exercise_id, student_id)
);

CREATE TABLE submission_answers (
    id                   BIGSERIAL PRIMARY KEY,
    submission_id        BIGINT    NOT NULL REFERENCES submissions (id) ON DELETE CASCADE,
    exercise_question_id BIGINT    NOT NULL REFERENCES exercise_questions (id) ON DELETE CASCADE,
    chosen_option        CHAR(1)   CHECK (chosen_option IN ('A', 'B', 'C', 'D')),
    is_correct           BOOLEAN,
    UNIQUE (submission_id, exercise_question_id)
);

CREATE INDEX ix_exercises_class ON exercises (class_id);
CREATE INDEX ix_submissions_student ON submissions (student_id);
