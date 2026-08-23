package com.yeager.shop.authentication.security;

import com.yeager.shop.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class AuthenticationVersionValidator implements OAuth2TokenValidator<Jwt> {
    private static final OAuth2Error INVALID_TOKEN = new OAuth2Error(
            "invalid_token",
            "The token is no longer valid",
            null
    );

    private final UserRepository userRepository;


    @Override
    public OAuth2TokenValidatorResult validate(Jwt token) {
        String subject = token.getSubject();

        Number tokenVersion = token.getClaim("authentication_version");

        if (subject == null || tokenVersion == null) {
            return OAuth2TokenValidatorResult.failure(INVALID_TOKEN);
        }

        Long userId;

        try {
            userId = Long.valueOf(subject);
        } catch (NumberFormatException exception) {
            return OAuth2TokenValidatorResult.failure(INVALID_TOKEN);
        }

        Optional<Integer> currentVersion = userRepository.findActiveAuthenticationVersion(userId);

        if (currentVersion.isEmpty()) {
            return OAuth2TokenValidatorResult.failure(INVALID_TOKEN);
        }

        if (currentVersion.get() != tokenVersion.intValue()) {
            return OAuth2TokenValidatorResult.failure(INVALID_TOKEN);
        }

        return OAuth2TokenValidatorResult.success();
    }
}
