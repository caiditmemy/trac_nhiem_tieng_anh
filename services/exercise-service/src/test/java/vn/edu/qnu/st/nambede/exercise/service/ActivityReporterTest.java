package vn.edu.qnu.st.nambede.exercise.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import vn.edu.qnu.st.nambede.exercise.clients.LearningClient;

class ActivityReporterTest {

    @Test
    void aCommittedSubmitIsReportedToLearningServiceWithTheStudentsToken() {
        LearningClient client = Mockito.mock(LearningClient.class);

        new ActivityReporter(client).onExerciseCompleted(new ExerciseCompleted(50L, "Bearer abc"));

        verify(client).reportExerciseCompleted(50L, "Bearer abc");
    }

    @Test
    void learningServiceBeingDownNeverFailsTheReport() {
        // Nothing listens on port 9: the call fails, which must be swallowed
        LearningClient client = new LearningClient("http://127.0.0.1:9");

        assertThatCode(() -> client.reportExerciseCompleted(50L, "Bearer abc")).doesNotThrowAnyException();
    }
}
