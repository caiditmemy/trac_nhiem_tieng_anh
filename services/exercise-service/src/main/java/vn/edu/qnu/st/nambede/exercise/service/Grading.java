package vn.edu.qnu.st.nambede.exercise.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Score out of 10 with two decimals, e.g. 8 of 10 correct gives 8.00 and 2 of 3 gives 6.67. */
public final class Grading {

    private Grading() {
    }

    public static BigDecimal scoreOutOf10(int correct, int total) {
        if (total <= 0) {
            return BigDecimal.ZERO.setScale(2);
        }
        return BigDecimal.valueOf(correct).multiply(BigDecimal.TEN).divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
    }
}
