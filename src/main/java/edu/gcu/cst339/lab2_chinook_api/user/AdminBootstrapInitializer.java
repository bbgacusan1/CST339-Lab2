package edu.gcu.cst339.lab2_chinook_api.user;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Creates the first administrator at startup when none exists
 * AdminBootstrapInitializer
 */
@Component
class AdminBootstrapInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapInitializer.class);

    private final UserRepository userRepository;
    private final String initialPassword;

    AdminBootstrapInitializer(UserRepository userRepository,
            @Value("${app.admin.initial-password}") String initialPassword) {
        this.userRepository = userRepository;
        this.initialPassword = initialPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.existsByRole("ADMIN")) {
            return;
        }
        if (initialPassword.isBlank()) {
            log.warn("No administrator exists and ADMIN_INITIAL_PASSWORD is not set; skipping bootstrap");
            return;
        }
        if (userRepository.existsByUsername("admin")) {
            log.warn("No administrator exists but the username admin is taken; skipping bootstrap");
            return;
        }
        User admin = new User("admin", initialPassword, "ADMIN");
        admin.setPasswordChangeRequired(true);
        userRepository.save(admin);
        log.info("Created bootstrap administrator");
    }
}
