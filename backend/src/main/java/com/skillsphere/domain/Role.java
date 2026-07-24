package com.skillsphere.domain;

/**
 * The two application roles used by the project.
 *
 * Keeping roles as a small enum is easier to explain than a separate role-management
 * module and is sufficient for the USER and ADMIN permissions required by SkillSphere.
 */
public enum Role {
    ROLE_USER,
    ROLE_ADMIN
}
