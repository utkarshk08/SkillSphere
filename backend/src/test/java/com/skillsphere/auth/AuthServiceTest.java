package com.skillsphere.auth;

import com.skillsphere.auth.dto.RegistrationRequest;
import com.skillsphere.auth.service.AuthService;
import com.skillsphere.domain.User;
import com.skillsphere.repository.UserRepository;
import com.skillsphere.security.JwtService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    @Test
    void registrationNormalizesUsernameForPortableUniqueness() {
        UserRepository userRepository = mock(UserRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        JwtService jwtService = mock(JwtService.class);
        AuthService service = new AuthService(
                userRepository,
                passwordEncoder,
                mock(AuthenticationManager.class),
                jwtService
        );
        RegistrationRequest request = new RegistrationRequest(
                "Test",
                "Student",
                "Alice_Dev",
                "Alice@example.com",
                "Strong@123",
                "Strong@123",
                "Example College",
                "B.Tech",
                "3rd Year",
                "India",
                null
        );
        when(passwordEncoder.encode(request.password())).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtService.generateToken(any(User.class))).thenReturn("token");
        when(jwtService.getExpirationSeconds()).thenReturn(1800L);

        service.register(request);

        verify(userRepository).existsByUsernameIgnoreCase("alice_dev");
        ArgumentCaptor<User> savedUser = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(savedUser.capture());
        assertEquals("alice_dev", savedUser.getValue().getUsername());
        assertEquals("alice@example.com", savedUser.getValue().getEmail());
    }
}
