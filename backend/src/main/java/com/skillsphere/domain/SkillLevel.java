package com.skillsphere.domain;

/**
 * A small, readable set of levels a student can select for a skill.
 *
 * An enum is used instead of accepting arbitrary text so the API and database
 * always use the same values and filtering remains predictable.
 */
public enum SkillLevel {
    BEGINNER,
    INTERMEDIATE,
    ADVANCED
}
