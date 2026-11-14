-- A due date only (no start time or timer).
ALTER TABLE exercises DROP COLUMN start_at;
ALTER TABLE exercises DROP COLUMN duration_minutes;
ALTER TABLE exercises ALTER COLUMN due_at SET NOT NULL;

-- Answer letters as VARCHAR(1), like learning-service (CHECK constraints are kept)
ALTER TABLE exercise_questions ALTER COLUMN correct_option TYPE VARCHAR(1);
