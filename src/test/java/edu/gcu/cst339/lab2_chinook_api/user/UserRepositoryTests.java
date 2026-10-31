package edu.gcu.cst339.lab2_chinook_api.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class UserRepositoryTests {

    @Autowired
    private UserRepository userRepository;

    @Test
    void saveAssignsGeneratedId() {
        User saved = userRepository.save(new User("repo-test-user", "temp", "USER"));

        assertThat(saved.getUserId()).isNotNull();
    }

    @Test
    void findByUsernameReturnsSavedUser() {
        userRepository.save(new User("repo-test-user", "temp", "USER"));

        assertThat(userRepository.findByUsername("repo-test-user")).isPresent();
    }

    @Test
    void findByUsernameReturnsEmptyWhenMissing() {
        assertThat(userRepository.findByUsername("repo-no-such-user")).isEmpty();
    }

    @Test
    void existsByUsernameDetectsDuplicate() {
        userRepository.save(new User("repo-test-user", "temp", "USER"));

        assertThat(userRepository.existsByUsername("repo-test-user")).isTrue();
        assertThat(userRepository.existsByUsername("repo-no-such-user")).isFalse();
    }

    @Test
    void databaseRejectsDuplicateUsername() {
        userRepository.saveAndFlush(new User("repo-test-user", "temp", "USER"));

        assertThatThrownBy(() -> userRepository.saveAndFlush(new User("repo-test-user", "other", "USER")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void existsByRoleFindsAdministrator() {
        userRepository.save(new User("repo-test-admin", "temp", "ADMIN"));

        assertThat(userRepository.existsByRole("ADMIN")).isTrue();
    }
}