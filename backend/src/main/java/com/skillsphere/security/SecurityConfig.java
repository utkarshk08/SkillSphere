package com.skillsphere.security;

import com.skillsphere.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;
import java.util.Locale;

/**
 * Central Spring Security configuration for a small, stateless REST application.
 *
 * Login credentials are verified by Spring's standard DaoAuthenticationProvider, and a JWT is
 * checked by JwtAuthenticationFilter on each later request. Server sessions are deliberately
 * disabled because the token carries the authenticated identity. This is easy to explain in an
 * interview: SecurityFilterChain chooses protected routes, UserDetailsService loads the account,
 * BCrypt checks a password, and the filter restores the SecurityContext from a valid token.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
    /**
     * Defines which routes are public and inserts the JWT filter before password authentication.
     * OAuth endpoints must be public because Google redirects a browser back to them before our
     * application has issued its own JWT.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            DaoAuthenticationProvider authenticationProvider,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            RestAuthenticationEntryPoint authenticationEntryPoint,
            RestAccessDeniedHandler accessDeniedHandler,
            CustomOAuth2UserService customOAuth2UserService,
            OAuth2AuthenticationSuccessHandler oauth2SuccessHandler,
            OAuth2AuthenticationFailureHandler oauth2FailureHandler
    )
            throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .authorizeHttpRequests(authorize -> authorize
                        // Authenticated "me" and "mine" routes must be checked before public GET patterns.
                        .requestMatchers(
                                "/api/auth/me",
                                "/api/profiles/me",
                                "/api/roadmaps/mine",
                                "/api/projects/mine",
                                "/api/skills/mine"
                        ).authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/auth/register", "/api/auth/login").permitAll()
                        .requestMatchers(
                                "/oauth2/**",
                                "/login/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/api-docs/**",
                                "/actuator/health",
                                "/actuator/health/**"
                        ).permitAll()
                        .requestMatchers(HttpMethod.GET, "/uploads/**").permitAll()
                        // A public profile/roadmap is meaningful only if it can be viewed without a JWT.
                        .requestMatchers(HttpMethod.GET,
                                "/api/profiles/**",
                                "/api/roadmaps/**",
                                "/api/projects/**",
                                "/api/communities/**",
                                "/api/skills/**",
                                "/api/announcements/**"
                        ).permitAll()
                        .anyRequest().authenticated())
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
                        .successHandler(oauth2SuccessHandler)
                        .failureHandler(oauth2FailureHandler))
                .authenticationProvider(authenticationProvider)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    /**
     * Loads one account type for both local login (email) and token validation (username).
     * Supporting both identifiers keeps the login form email-based while keeping JWT's subject
     * human-readable as the requested username.
     */
    @Bean
    public UserDetailsService userDetailsService(UserRepository userRepository) {
        return identifier -> {
            String normalizedIdentifier = identifier == null ? "" : identifier.trim();
            return userRepository.findByEmail(normalizedIdentifier.toLowerCase(Locale.ROOT))
                    .or(() -> userRepository.findByUsernameIgnoreCase(normalizedIdentifier))
                    .orElseThrow(() -> new UsernameNotFoundException("User account was not found."));
        };
    }

    /**
     * BCrypt is intentionally used instead of reversible encryption. Passwords are stored as a
     * one-way adaptive hash, so a database leak does not directly reveal the original password.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Spring's built-in provider combines UserDetailsService and PasswordEncoder for local login.
     * A custom provider would add code without improving the project for this use case.
     */
    @Bean
    public DaoAuthenticationProvider authenticationProvider(
            UserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder
    ) {
        DaoAuthenticationProvider authenticationProvider = new DaoAuthenticationProvider(userDetailsService);
        authenticationProvider.setPasswordEncoder(passwordEncoder);
        return authenticationProvider;
    }

    /**
     * AuthService delegates login to this manager, making the requested authentication flow
     * explicit: controller -> service -> AuthenticationManager -> DaoAuthenticationProvider.
     */
    @Bean
    public AuthenticationManager authenticationManager(DaoAuthenticationProvider authenticationProvider) {
        return new ProviderManager(authenticationProvider);
    }

    /**
     * Lets the Vite application at localhost:5173 call the REST API from a browser. The origin is
     * configurable so the same code can later be used with a deployed frontend URL.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${app.frontend-url}") String frontendUrl
    ) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(frontendUrl));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        configuration.setExposedHeaders(List.of("Authorization"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
