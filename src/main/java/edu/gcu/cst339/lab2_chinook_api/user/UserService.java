package edu.gcu.cst339.lab2_chinook_api.user;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private static final Set<String> ROLES = Set.of("USER", "ADMIN");

    private final UserRepository userRepository;

    UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    void register(RegistrationDto dto) {
        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            throw new UserValidationException("Passwords do not match");
        }
        if (userRepository.existsByUsername(dto.getUsername())) {
            throw new UserValidationException("That username is already taken");
        }
        User saved = userRepository.save(new User(dto.getUsername(), dto.getPassword(), "USER"));
        log.info("Registered user {}", saved.getUsername());
    }

    @Transactional(readOnly = true)
    Optional<UserSummaryDto> findByUsername(String username) {
        log.debug("Finding user {}", username);
        return userRepository.findByUsername(username)
                .map(UserSummaryDto::fromEntity);
    }

    @Transactional(readOnly = true)
    Optional<UserSummaryDto> validateLogin(LoginDto dto) {
        return userRepository.findByUsername(dto.getUsername())
                .filter(User::isEnabled)
                .filter(user -> user.getPassword().equals(dto.getPassword()))
                .map(UserSummaryDto::fromEntity);
    }

    @Transactional(readOnly = true)
    List<UserSummaryDto> findAll() {
        log.debug("Finding all users");
        return userRepository.findAll().stream()
                .map(UserSummaryDto::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    UserSummaryDto findById(Integer id) {
        log.debug("Finding user {}", id);
        return userRepository.findById(id)
                .map(UserSummaryDto::fromEntity)
                .orElseThrow(() -> new UserNotFoundException(id));
    }

    @Transactional
    void updateAccount(Integer id, UserUpdateDto dto) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
        if (!ROLES.contains(dto.getRole())) {
            throw new UserValidationException("Invalid role");
        }
        boolean isLastAdmin = "ADMIN".equals(user.getRole())
                && user.isEnabled()
                && userRepository.countByRoleAndEnabled("ADMIN", true) <= 1;
        boolean losingAdmin = !"ADMIN".equals(dto.getRole()) || !dto.isEnabled();
        if (isLastAdmin && losingAdmin) {
            throw new UserValidationException("Cannot remove the only administrator");
        }
        user.setRole(dto.getRole());
        user.setEnabled(dto.isEnabled());
        user.setPasswordChangeRequired(dto.isPasswordChangeRequired());
        userRepository.save(user);
        log.info("Updated user {}", id);
    }

    @Transactional
    void changePassword(String username, PasswordChangeDto dto) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserValidationException("User not found"));
        if (!user.getPassword().equals(dto.getCurrentPassword())) {
            throw new UserValidationException("Current password is incorrect");
        }
        if (!dto.getNewPassword().equals(dto.getConfirmPassword())) {
            throw new UserValidationException("Passwords do not match");
        }
        user.setPassword(dto.getNewPassword());
        user.setPasswordChangeRequired(false);
        userRepository.save(user);
        log.info("Changed password for user {}", username);
    }
}
