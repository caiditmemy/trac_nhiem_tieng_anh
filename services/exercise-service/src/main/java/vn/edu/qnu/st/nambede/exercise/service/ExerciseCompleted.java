package vn.edu.qnu.st.nambede.exercise.service;

/** Published inside the submit transaction; {@link ActivityReporter} acts on it once the submit is committed. */
public record ExerciseCompleted(Long submissionId, String authorization) {
}
