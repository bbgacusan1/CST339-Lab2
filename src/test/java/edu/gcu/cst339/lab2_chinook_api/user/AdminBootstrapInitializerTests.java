package edu.gcu.cst339.lab2_chinook_api.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AdminBootstrapInitializerTests {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private AdminBootstrapInitializer initializer(String initialPassword) {
        return new AdminBootstrapInitializer(userRepository, passwordEncoder, initialPassword);
    }

    // The bootstrap admin must be saved with encode(initialPassword), never the plain value,
    // otherwise BCrypt login could never match it.
    @Test
    void createsAdminWithEncodedPasswordWhenNoneExists() {
        when(userRepository.existsByRole("ADMIN")).thenReturn(false);
        when(userRepository.existsByUsername("admin")).thenReturn(false);
        when(passwordEncoder.encode("temporary")).thenReturn("encoded-temporary");

        initializer("temporary").run(null);

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertThat(saved.getValue().getUsername()).isEqualTo("admin");
        assertThat(saved.getValue().getPassword()).isEqualTo("encoded-temporary").isNotEqualTo("temporary");
        assertThat(saved.getValue().getRole()).isEqualTo("ADMIN");
        assertThat(saved.getValue().isEnabled()).isTrue();
        assertThat(saved.getValue().isPasswordChangeRequired()).isTrue();
    }

    @Test
    void doesNothingWhenAdminExists() {
        when(userRepository.existsByRole("ADMIN")).thenReturn(true);

        initializer("temporary").run(null);

        verify(passwordEncoder, never()).encode(any());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void skipsWhenPasswordIsBlank() {
        when(userRepository.existsByRole("ADMIN")).thenReturn(false);

        initializer("").run(null);

        verify(passwordEncoder, never()).encode(any());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void skipsWhenUsernameIsTaken() {
        when(userRepository.existsByRole("ADMIN")).thenReturn(false);
        when(userRepository.existsByUsername("admin")).thenReturn(true);

        initializer("temporary").run(null);

        verify(passwordEncoder, never()).encode(any());
        verify(userRepository, never()).save(any(User.class));
    }
}
