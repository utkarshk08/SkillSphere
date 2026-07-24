package com.skillsphere.domain;

/**
 * Explains whether a skill is offered for teaching or is currently being learned.
 * Keeping the two choices explicit makes matching students easy to describe in an
 * interview without adding a separate matching engine.
 */
public enum SkillIntent {
    TEACH,
    LEARN
}
