package com.skillsphere;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Application entry point for SkillSphere.
 *
 * Spring Boot scans the com.skillsphere package below this class, which keeps each feature
 * module (auth, profile, project, and so on) discoverable without manual registration.
 */
@SpringBootApplication
public class SkillSphereApplication {

    public static void main(String[] args) {
        SpringApplication.run(SkillSphereApplication.class, args);
    }
}
