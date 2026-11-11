package vn.edu.qnu.st.nambede.exercise.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class GradingTest {

    @ParameterizedTest
    @CsvSource({"8, 10, 8.00", "1, 3, 3.33", "2, 3, 6.67", "0, 5, 0.00", "5, 5, 10.00", "0, 0, 0.00"})
    void scoreIsOutOf10WithTwoDecimals(int correct, int total, String expected) {
        assertThat(Grading.scoreOutOf10(correct, total)).isEqualByComparingTo(expected);
        assertThat(Grading.scoreOutOf10(correct, total).scale()).isEqualTo(2);
    }
}
