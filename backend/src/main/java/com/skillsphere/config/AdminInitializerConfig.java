package com.skillsphere.config;

import com.skillsphere.domain.AuthProvider;
import com.skillsphere.domain.Role;
import com.skillsphere.domain.User;
import com.skillsphere.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Creates the one initial administrator without exposing an unsafe public admin-registration API.
 *
 * CommandLineRunner runs after Spring has prepared the application context. The role existence
 * check makes the operation idempotent: later restarts do not create duplicate administrators.
 * For a real deployment, change the seeded password immediately after first sign-in.
 */
@Configuration
public class AdminInitializerConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdminInitializerConfig.class);

    @Bean
    CommandLineRunner createInitialAdmin(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return arguments -> {
            if (userRepository.existsByRole(Role.ROLE_ADMIN)) {
                return;
            }

            User admin = new User();
            admin.setFirstName("SkillSphere");
            admin.setLastName("Admin");
            admin.setUsername("admin");
            admin.setEmail("admin@skillsphere.com");
            admin.setPassword(passwordEncoder.encode("Admin@12345"));
            admin.setRole(Role.ROLE_ADMIN);
            admin.setAuthProvider(AuthProvider.LOCAL);
            admin.setVerified(true);

            userRepository.save(admin);
            LOGGER.warn("Created the initial SkillSphere admin account. Change its default password after first login.");
        };
    }
}
