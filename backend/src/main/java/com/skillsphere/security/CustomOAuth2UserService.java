package com.skillsphere.security;

import com.skillsphere.auth.service.AuthService;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

/**
 * Bridges Google's profile response to the local User table.
 *
 * DefaultOAuth2UserService already handles the OAuth protocol and user-info request. This class
 * only maps the returned Google attributes to a local account, which is much simpler and safer
 * than reimplementing OAuth2 communication ourselves.
 */
@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final AuthService authService;

    public CustomOAuth2UserService(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauthUser = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        if (!"google".equalsIgnoreCase(registrationId)) {
            throw new OAuth2AuthenticationException(new OAuth2Error("unsupported_provider"));
        }

        String email = oauthUser.getAttribute("email");
        if (email == null || email.isBlank()) {
            throw new OAuth2AuthenticationException(new OAuth2Error("missing_email"));
        }

        authService.registerOrGetGoogleUser(
                email,
                oauthUser.getAttribute("given_name"),
                oauthUser.getAttribute("family_name")
        );
        return oauthUser;
    }
}
