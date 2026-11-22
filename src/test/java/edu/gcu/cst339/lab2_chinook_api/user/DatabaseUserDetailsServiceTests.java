package edu.gcu.cst339.lab2_chinook_api.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@ExtendWith(MockitoExtension.class)
class DatabaseUserDetailsServiceTests {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private DatabaseUserDetailsService userDetailsService;

    @Test
    void loadsUsernameAndStoredPasswordFromRepository() {
        User user = new User("alice", "stored-password-value", "USER");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

        UserDetails details = userDetailsService.loadUserByUsername("alice");

        assertThat(details.getUsername()).isEqualTo("alice");
        assertThat(details.getPassword()).isEqualTo("stored-password-value");
    }

    @Test
    void mapsUserRoleToRoleUserAuthority() {
        User user = new User("alice", "x", "USER");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

        UserDetails details = userDetailsService.loadUserByUsername("alice");

        assertThat(details.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_USER");
    }

    @Test
    void mapsAdminRoleToRoleAdminAuthority() {
        User user = new User("admin", "x", "ADMIN");
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));

        UserDetails details = userDetailsService.loadUserByUsername("admin");

        assertThat(details.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_ADMIN")
                .doesNotContain("ROLE_ROLE_ADMIN");
    }

    @Test
    void enabledUserIsEnabled() {
        User user = new User("alice", "x", "USER");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

        UserDetails details = userDetailsService.loadUserByUsername("alice");

        assertThat(details.isEnabled()).isTrue();
    }

    @Test
    void disabledUserIsNotEnabled() {
        User user = new User("bob", "x", "USER");
        user.setEnabled(false);
        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(user));

        UserDetails details = userDetailsService.loadUserByUsername("bob");

        assertThat(details.isEnabled()).isFalse();
    }

    @Test
    void unknownUsernameThrowsUsernameNotFoundException() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("ghost"))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}
