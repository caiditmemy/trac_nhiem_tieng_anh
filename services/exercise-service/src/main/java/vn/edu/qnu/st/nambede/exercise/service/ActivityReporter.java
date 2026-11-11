package vn.edu.qnu.st.nambede.exercise.service;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import vn.edu.qnu.st.nambede.exercise.clients.LearningClient;

/**
 * Once a submit is committed, tells learning-service so the student gets activity points and a streak day.
 * Runs after the commit: a submit that fails never gives points, and learning-service being down never fails a submit.
 */
@Component
class ActivityReporter {

    private final LearningClient learningClient;

    ActivityReporter(LearningClient learningClient) {
        this.learningClient = learningClient;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void onExerciseCompleted(ExerciseCompleted event) {
        learningClient.reportExerciseCompleted(event.submissionId(), event.authorization());
    }
}
