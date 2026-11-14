package vn.edu.qnu.st.nambede.exercise.jwt;

import java.security.PublicKey;

/** The public keys that verify tokens, by key id ("kid" in the token's header). */
@FunctionalInterface
public interface PublicKeys {

    /**
     * The key with this id, or null when auth-service has no such key.
     *
     * @throws KeysUnavailableException when the keys cannot be read from auth-service and none are known yet
     */
    PublicKey find(String kid);
}
