package com.skillsphere.domain;

/**
 * Identifies how an account was first authenticated.
 *
 * This lets the same User entity support both password login and the requested Google
 * OAuth login without introducing a second account table.
 */
public enum AuthProvider {
    LOCAL,
    GOOGLE
}
