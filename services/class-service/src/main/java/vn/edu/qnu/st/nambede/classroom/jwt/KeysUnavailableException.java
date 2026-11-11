package vn.edu.qnu.st.nambede.classroom.jwt;

/** auth-service cannot be reached for its public keys: no token can be checked yet (502, not 401). */
public class KeysUnavailableException extends RuntimeException {

    public KeysUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
