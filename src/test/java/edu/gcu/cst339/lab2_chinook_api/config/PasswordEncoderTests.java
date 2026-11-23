package edu.gcu.cst339.lab2_chinook_api.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

class PasswordEncoderTests {

    // Plain unit test: no Spring context, just call the bean method directly.
    private final PasswordEncoder encoder = new SecurityConfig().passwordEncoder();

    @Test
    void encodedPasswordIsNotPlainText() {
        String hash = encoder.encode("password");

        assertThat(hash).isNotEqualTo("password").startsWith("$2");
    }

    @Test
    void hashHasBCryptLength() {
        String hash = encoder.encode("password");

        assertThat(hash).hasSize(60);
    }

    @Test
    void correctPasswordMatchesHash() {
        String hash = encoder.encode("password");

        assertThat(encoder.matches("password", hash)).isTrue();
    }

    @Test
    void wrongPasswordDoesNotMatchHash() {
        String hash = encoder.encode("password");

        assertThat(encoder.matches("wrong", hash)).isFalse();
    }

    // BCrypt adds a random salt to every encode, so the same password never produces
    // the same hash twice. matches() still works because the salt is stored in the hash.
    @Test
    void samePasswordProducesDifferentHashesBecauseOfSalt() {
        String first = encoder.encode("password");
        String second = encoder.encode("password");

        assertThat(first).isNotEqualTo(second);
        assertThat(encoder.matches("password", first)).isTrue();
        assertThat(encoder.matches("password", second)).isTrue();
    }
}
