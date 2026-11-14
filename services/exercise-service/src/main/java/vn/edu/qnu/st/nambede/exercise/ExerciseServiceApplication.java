package vn.edu.qnu.st.nambede.exercise;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;

// Callers are authenticated by the JWT filter, so Spring's default in-memory user is not needed
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class ExerciseServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ExerciseServiceApplication.class, args);
    }
}
