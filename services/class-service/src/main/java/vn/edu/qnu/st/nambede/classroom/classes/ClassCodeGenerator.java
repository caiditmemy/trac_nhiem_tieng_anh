package vn.edu.qnu.st.nambede.classroom.classes;

import java.security.SecureRandom;
import org.springframework.stereotype.Component;

/** Short codes that students type to join a class, e.g. "K7M2QX". */
@Component
public class ClassCodeGenerator {

    // No 0/O or 1/I/L: easy to read from the board and to type
    static final String ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    static final int LENGTH = 6;

    private final SecureRandom random = new SecureRandom();

    public String next() {
        StringBuilder code = new StringBuilder(LENGTH);
        for (int i = 0; i < LENGTH; i++) {
            code.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return code.toString();
    }
}
