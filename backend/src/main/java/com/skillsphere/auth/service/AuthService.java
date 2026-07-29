package com.skillsphere.auth.service;

import com.skillsphere.auth.dto.AuthResponse;
import com.skillsphere.auth.dto.LoginRequest;
import com.skillsphere.auth.dto.RegistrationRequest;
import com.skillsphere.auth.dto.UserResponse;
import com.skillsphere.domain.AuthProvider;
import com.skillsphere.domain.Role;
import com.skillsphere.domain.User;
import com.skillsphere.exception.BadRequestException;
import com.skillsphere.exception.ResourceNotFoundException;
import com.skillsphere.repository.UserRepository;
import com.skillsphere.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

/**
 * Service layer for local registration, local login, and Google-account provisioning.
 *
 * Keeping this logic out of AuthController preserves the requested controller -> service ->
 * repository architecture. Spring Security performs password verification through
 * AuthenticationManager; this service only coordinates validation rules, persistence, and JWT
 * generation.
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    /** Creates a ROLE_USER account and immediately returns a JWT for the new student. */
    @Transactional
    public AuthResponse register(RegistrationRequest request) {
        String normalizedEmail = normalizeEmail(request.email());
        String username = normalizeUsername(request.username());

        if (!request.password().equals(request.confirmPassword())) {
            throw new BadRequestException("Password and confirm password must match.");
        }
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new BadRequestException("An account with this email already exists.");
        }
        if (userRepository.existsByUsernameIgnoreCase(username)) {
            throw new BadRequestException("This username is already taken.");
        }

        User user = new User();
        user.setFirstName(trim(request.firstName()));
        user.setLastName(trim(request.lastName()));
        user.setUsername(username);
        user.setEmail(normalizedEmail);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setCollegeName(trim(request.collegeName()));
        user.setCourse(trim(request.course()));
        user.setYearOfStudy(trim(request.yearOfStudy()));
        user.setCountry(trim(request.country()));
        user.setBio(request.bio() == null ? null : trim(request.bio()));
        user.setRole(Role.ROLE_USER);
        user.setAuthProvider(AuthProvider.LOCAL);

        return createAuthResponse(userRepository.save(user));
    }

    /**
     * Delegates credential checking to Spring Security rather than manually comparing hashes.
     * DaoAuthenticationProvider calls UserDetailsService and BCryptPasswordEncoder internally.
     */
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String normalizedEmail = normalizeEmail(request.email());
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(normalizedEmail, request.password())
        );

        User user = findUserByEmail(normalizedEmail);
        return createAuthResponse(user);
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(String username) {
        return UserResponse.from(findUserByUsername(username));
    }

    /** Used by the OAuth success handler after Google has returned a verified identity. */
    @Transactional(readOnly = true)
    public User findUserByEmail(String email) {
        return userRepository.findByEmail(normalizeEmail(email))
                .orElseThrow(() -> new ResourceNotFoundException("User account was not found."));
    }

    /**
     * Creates the minimal local record for a first-time Google user. Google supplies only identity
     * data, so optional profile details can be completed later through the profile module.
     */
    @Transactional
    public User registerOrGetGoogleUser(String email, String firstName, String lastName) {
        String normalizedEmail = normalizeEmail(email);
        return userRepository.findByEmail(normalizedEmail).orElseGet(() -> {
            User user = new User();
            user.setEmail(normalizedEmail);
            user.setUsername(createUniqueGoogleUsername(normalizedEmail));
            user.setFirstName(blankToNull(firstName));
            user.setLastName(blankToNull(lastName));
            user.setRole(Role.ROLE_USER);
            user.setAuthProvider(AuthProvider.GOOGLE);
            return userRepository.save(user);
        });
    }

    private AuthResponse createAuthResponse(User user) {
        return new AuthResponse(
                jwtService.generateToken(user),
                "Bearer",
                jwtService.getExpirationSeconds(),
                UserResponse.from(user)
        );
    }

    private User findUserByUsername(String username) {
        return userRepository.findByUsernameIgnoreCase(normalizeUsername(username))
                .orElseThrow(() -> new ResourceNotFoundException("User account was not found."));
    }

    private String createUniqueGoogleUsername(String email) {
        String localPart = email.substring(0, email.indexOf('@'))
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9_]", "_");
        String baseUsername = localPart.isBlank() ? "google_user" : localPart;
        baseUsername = baseUsername.substring(0, Math.min(baseUsername.length(), 44));

        String candidate = baseUsername;
        int suffix = 1;
        while (userRepository.existsByUsernameIgnoreCase(candidate)) {
            candidate = baseUsername + "_" + suffix++;
        }
        return candidate;
    }

    private String normalizeEmail(String email) {
        return trim(email).toLowerCase(Locale.ROOT);
    }

    private String normalizeUsername(String username) {
        return trim(username).toLowerCase(Locale.ROOT);
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private String blankToNull(String value) {
        String trimmedValue = trim(value);
        return trimmedValue.isBlank() ? null : trimmedValue;
    }
}
