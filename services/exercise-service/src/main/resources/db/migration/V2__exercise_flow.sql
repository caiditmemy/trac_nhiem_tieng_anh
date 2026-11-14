-- Phase 4 decisions: one attempt per student, a due date only (no timer), score out of 10.
ALTER TABLE exercises DROP COLUMN start_at;
ALTER TABLE exercises DROP COLUMN duration_minutes;
ALTER TABLE exercises ALTER COLUMN due_at SET NOT NULL;

-- Answer letters as VARCHAR(1), like learning-service (CHECK constraints are kept)
ALTER TABLE exercise_questions ALTER COLUMN correct_option TYPE VARCHAR(1);
ALTER TABLE submission_answers ALTER COLUMN chosen_option TYPE VARCHAR(1);

-- Number of correct answers, saved when the student submits (e.g. 8 of 10)
ALTER TABLE submissions ADD COLUMN correct_count INT;
