package com.skillsphere.security;

import com.skillsphere.auth.service.AuthService;
import com.skillsphere.domain.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

/**
 * Finishes Google sign-in by issuing the same application JWT used after local login.
 *
 * Google authenticates the identity; SkillSphere then creates its own short-lived token and
 * redirects the browser to the React callback route. This keeps the React app's later API calls
 * identical regardless of whether the user chose password login or Google login.
 */
@Component
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final AuthService authService;
    private final JwtService jwtService;
    private final String frontendRedirectUrl;

    public OAuth2AuthenticationSuccessHandler(
            AuthService authService,
            JwtService jwtService,
            @Value("${app.oauth2.authorized-redirect-uri}") String frontendRedirectUrl
    ) {
        this.authService = authService;
        this.jwtService = jwtService;
        this.frontendRedirectUrl = frontendRedirectUrl;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {
        if (!(authentication.getPrincipal() instanceof OAuth2User oauthUser)) {
            redirectWithError(request, response);
            return;
        }

        String email = oauthUser.getAttribute("email");
        if (email == null || email.isBlank()) {
            redirectWithError(request, response);
            return;
        }

        User user = authService.findUserByEmail(email);
        String token = jwtService.generateToken(user);
        String targetUrl = UriComponentsBuilder.fromUriString(frontendRedirectUrl)
                .queryParam("token", token)
                .build()
                .encode()
                .toUriString();

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    private void redirectWithError(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String targetUrl = UriComponentsBuilder.fromUriString(frontendRedirectUrl)
                .queryParam("error", "oauth")
                .queryParam("message", "Google sign-in could not be completed.")
                .build()
                .encode()
                .toUriString();
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
