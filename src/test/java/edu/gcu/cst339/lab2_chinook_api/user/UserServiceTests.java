package edu.gcu.cst339.lab2_chinook_api.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserServiceTests {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private RegistrationDto registration(String username, String password, String confirmPassword) {
        RegistrationDto dto = new RegistrationDto();
        dto.setUsername(username);
        dto.setPassword(password);
        dto.setConfirmPassword(confirmPassword);
        return dto;
    }

    private LoginDto login(String username, String password) {
        LoginDto dto = new LoginDto();
        dto.setUsername(username);
        dto.setPassword(password);
        return dto;
    }

    private PasswordChangeDto passwordChange(String current, String next, String confirm) {
        PasswordChangeDto dto = new PasswordChangeDto();
        dto.setCurrentPassword(current);
        dto.setNewPassword(next);
        dto.setConfirmPassword(confirm);
        return dto;
    }

    @Test
    void registerSavesNewUserWithUserRole() {
        when(userRepository.save(any(User.class))).thenAnswer(call -> call.getArgument(0));

        userService.register(registration("newuser", "secret", "secret"));

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertThat(saved.getValue().getUsername()).isEqualTo("newuser");
        assertThat(saved.getValue().getRole()).isEqualTo("USER");
        assertThat(saved.getValue().isEnabled()).isTrue();
    }

    @Test
    void registerRejectsMismatchedPasswords() {
        assertThatThrownBy(() -> userService.register(registration("newuser", "secret", "different")))
                .isInstanceOf(UserValidationException.class);

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void registerRejectsDuplicateUsername() {
        when(userRepository.existsByUsername("taken")).thenReturn(true);

        assertThatThrownBy(() -> userService.register(registration("taken", "secret", "secret")))
                .isInstanceOf(UserValidationException.class);

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void validateLoginReturnsUserForValidCredentials() {
        when(userRepository.findByUsername("member"))
                .thenReturn(Optional.of(new User("member", "secret", "USER")));

        Optional<UserSummaryDto> result = userService.validateLogin(login("member", "secret"));

        assertThat(result).isPresent();
        assertThat(result.get().username()).isEqualTo("member");
    }

    @Test
    void validateLoginRejectsWrongPassword() {
        when(userRepository.findByUsername("member"))
                .thenReturn(Optional.of(new User("member", "secret", "USER")));

        assertThat(userService.validateLogin(login("member", "wrong"))).isEmpty();
    }

    @Test
    void validateLoginRejectsUnknownUser() {
        when(userRepository.findByUsername("nobody")).thenReturn(Optional.empty());

        assertThat(userService.validateLogin(login("nobody", "secret"))).isEmpty();
    }

    @Test
    void validateLoginRejectsDisabledUser() {
        User user = new User("member", "secret", "USER");
        user.setEnabled(false);
        when(userRepository.findByUsername("member")).thenReturn(Optional.of(user));

        assertThat(userService.validateLogin(login("member", "secret"))).isEmpty();
    }

    @Test
    void updateAccountChangesFields() {
        User user = new User("member", "secret", "USER");
        when(userRepository.findById(1)).thenReturn(Optional.of(user));

        UserUpdateDto dto = new UserUpdateDto();
        dto.setRole("ADMIN");
        dto.setEnabled(false);
        dto.setPasswordChangeRequired(true);
        userService.updateAccount(1, dto);

        assertThat(user.getRole()).isEqualTo("ADMIN");
        assertThat(user.isEnabled()).isFalse();
        assertThat(user.isPasswordChangeRequired()).isTrue();
        verify(userRepository).save(user);
    }

    @Test
    void updateAccountThrowsWhenUserMissing() {
        when(userRepository.findById(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateAccount(999, new UserUpdateDto()))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void changePasswordUpdatesPasswordAndClearsFlag() {
        User user = new User("member", "old", "USER");
        user.setPasswordChangeRequired(true);
        when(userRepository.findByUsername("member")).thenReturn(Optional.of(user));

        userService.changePassword("member", passwordChange("old", "new", "new"));

        assertThat(user.getPassword()).isEqualTo("new");
        assertThat(user.isPasswordChangeRequired()).isFalse();
        verify(userRepository).save(user);
    }

    @Test
    void changePasswordRejectsWrongCurrentPassword() {
        when(userRepository.findByUsername("member"))
                .thenReturn(Optional.of(new User("member", "old", "USER")));

        assertThatThrownBy(() -> userService.changePassword("member", passwordChange("wrong", "new", "new")))
                .isInstanceOf(UserValidationException.class);

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void changePasswordRejectsMismatchedNewPasswords() {
        lenient().when(userRepository.findByUsername("member"))
                .thenReturn(Optional.of(new User("member", "old", "USER")));

        assertThatThrownBy(() -> userService.changePassword("member", passwordChange("old", "new", "different")))
                .isInstanceOf(UserValidationException.class);

        verify(userRepository, never()).save(any(User.class));
    }

    private UserUpdateDto update(String role, boolean enabled) {
        UserUpdateDto dto = new UserUpdateDto();
        dto.setRole(role);
        dto.setEnabled(enabled);
        return dto;
    }

    @Test
    void updateAccountRejectsInvalidRole() {
        when(userRepository.findById(1)).thenReturn(Optional.of(new User("member", "secret", "USER")));

        assertThatThrownBy(() -> userService.updateAccount(1, update("SUPERUSER", true)))
                .isInstanceOf(UserValidationException.class);

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateAccountRejectsDisablingLastAdmin() {
        when(userRepository.findById(1)).thenReturn(Optional.of(new User("boss", "secret", "ADMIN")));
        when(userRepository.countByRoleAndEnabled("ADMIN", true)).thenReturn(1L);

        assertThatThrownBy(() -> userService.updateAccount(1, update("ADMIN", false)))
                .isInstanceOf(UserValidationException.class);

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateAccountRejectsDemotingLastAdmin() {
        when(userRepository.findById(1)).thenReturn(Optional.of(new User("boss", "secret", "ADMIN")));
        when(userRepository.countByRoleAndEnabled("ADMIN", true)).thenReturn(1L);

        assertThatThrownBy(() -> userService.updateAccount(1, update("USER", true)))
                .isInstanceOf(UserValidationException.class);

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateAccountAllowsDisablingAdminWhenAnotherExists() {
        User admin = new User("boss", "secret", "ADMIN");
        when(userRepository.findById(1)).thenReturn(Optional.of(admin));
        when(userRepository.countByRoleAndEnabled("ADMIN", true)).thenReturn(2L);

        userService.updateAccount(1, update("ADMIN", false));

        assertThat(admin.isEnabled()).isFalse();
    }
}
