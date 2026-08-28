package com.yeager.shop.authentication.service;

import com.yeager.shop.authentication.dto.*;
import com.yeager.shop.authentication.entity.Session;
import com.yeager.shop.authentication.entity.SessionStatus;
import com.yeager.shop.authentication.repository.SessionRepository;
import com.yeager.shop.authentication.security.GeneratedRefreshToken;
import com.yeager.shop.authentication.security.JwtProperties;
import com.yeager.shop.authentication.security.JwtService;
import com.yeager.shop.authentication.security.RefreshTokenService;
import com.yeager.shop.common.exception.*;
import com.yeager.shop.user.entity.User;
import com.yeager.shop.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthenticationService {
    private final UserRepository userRepository;
    private final SessionRepository sessionRepository;

    private final RefreshTokenService refreshTokenService;

    private final PasswordEncoder passwordEncoder;

    private final JwtService jwtService;
    private final JwtProperties jwtProperties;

    @Transactional
    public SignUpResponse signUp(SignUpRequest request) {
        String email = normalizeEmail(request.getEmail());

        if (userRepository.existsByEmail(email)) {
            throw new ResourceAlreadyExistsException("User with this email already exists");
        }

        User user = new User();

        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(request.getFirstName().trim());
        user.setLastName(request.getLastName().trim());

        User savedUser = userRepository.save(user);

        return new SignUpResponse(
                savedUser.getUserId(),
                savedUser.getEmail(),
                savedUser.getFirstName(),
                savedUser.getLastName()
        );
    }

    @Transactional
    public AuthenticationResult signIn(SignInRequest request) {
        String email = normalizeEmail(request.getEmail());

        User user = userRepository.findByEmail(email)
                .orElseThrow(InvalidCredentialsException::new);

        if (!user.isActive()) {
            throw new InvalidCredentialsException();
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        String accessToken = jwtService.generateAccessToken(user);
        GeneratedRefreshToken refreshToken = refreshTokenService.generate();

        Instant now = Instant.now();

        Session session = new Session();

        session.setUser(user);
        session.setJti(refreshToken.getJti());
        session.setTokenHash(refreshToken.getTokenHash());
        session.setStatus(SessionStatus.ACTIVE);
        session.setIssuedAt(now);
        session.setExpiresAt(now.plus(jwtProperties.getRefreshTokenTtl()));

        sessionRepository.save(session);

        AccessTokenResponse response = new AccessTokenResponse(
                accessToken,
                "Bearer",
                jwtProperties
                        .getAccessTokenTtl()
                        .toSeconds()
        );

        return new AuthenticationResult(
                response,
                refreshToken.getToken()
        );
    }

    @Transactional(
            noRollbackFor = RefreshTokenReuseException.class
    )
    public AuthenticationResult refreshTokens(String rawRefreshToken) {
        String jti = refreshTokenService.extractJti(rawRefreshToken);

        Long userId = sessionRepository.findUserIdByJti(jti)
                .orElseThrow(InvalidCredentialsException::new);
        
        User user = userRepository.findForUpdateById(userId)
                .orElseThrow(InvalidCredentialsException::new);

        Session currentSession = sessionRepository
                .findForUpdateByJti(jti)
                .orElseThrow(InvalidCredentialsException::new);

        if (!refreshTokenService.matches(
                rawRefreshToken,
                currentSession.getTokenHash()
        )) {
            throw new InvalidCredentialsException();
        }

        if (currentSession.getStatus() == SessionStatus.REUSED) {
            throw new RefreshTokenReuseException();
        }

        if (currentSession.getStatus() == SessionStatus.REVOKED) {

            if (currentSession.getReplacedBy() != null) {
                handleRefreshTokenReuse(currentSession);

                throw new RefreshTokenReuseException();
            }

            throw new InvalidCredentialsException();
        }

        Instant now = Instant.now();

        if (!currentSession.getExpiresAt().isAfter(now)) {
            throw new InvalidCredentialsException();
        }

        if (!user.isActive()) {
            throw new InvalidCredentialsException();
        }

        GeneratedRefreshToken newRefreshToken = refreshTokenService.generate();

        Session newSession = new Session();

        newSession.setUser(user);
        newSession.setJti(newRefreshToken.getJti());
        newSession.setTokenHash(newRefreshToken.getTokenHash());
        newSession.setStatus(SessionStatus.ACTIVE);
        newSession.setIssuedAt(now);
        newSession.setExpiresAt(now.plus(jwtProperties.getRefreshTokenTtl()));

        sessionRepository.save(newSession);

        currentSession.setStatus(SessionStatus.REVOKED);
        currentSession.setReplacedBy(newSession);

        String accessToken = jwtService.generateAccessToken(user);

        AccessTokenResponse response = new AccessTokenResponse(
                accessToken,
                "Bearer",
                jwtProperties
                        .getAccessTokenTtl()
                        .toSeconds()
        );

        return new AuthenticationResult(
                response,
                newRefreshToken.getToken()
        );
    }

    @Transactional
    public void signOut(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            return;
        }

        String jti;

        try {
            jti = refreshTokenService.extractJti(rawRefreshToken);
        } catch (InvalidCredentialsException exception) {
            return;
        }

        Optional<Session> optionalSession = sessionRepository.findForUpdateByJti(jti);

        if (optionalSession.isEmpty()) {
            return;
        }

        Session session = optionalSession.get();

        if (!refreshTokenService.matches(
                rawRefreshToken,
                session.getTokenHash()
        )) {
            return;
        }

        if (session.getStatus() == SessionStatus.ACTIVE) {
            session.setStatus(SessionStatus.REVOKED);
        }
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = userRepository
                .findForUpdateById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found by id: " + userId)
                );

        if (!passwordEncoder.matches(
                request.getCurrentPassword(),
                user.getPasswordHash()
        )) {
            throw new InvalidOperationException("Current password is incorrect");
        }

        if (passwordEncoder.matches(
                request.getNewPassword(),
                user.getPasswordHash()
        )) {
            throw new InvalidOperationException("New password must be different from current password");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));

        user.setAuthenticationVersion(user.getAuthenticationVersion() + 1);

        sessionRepository.revokeActiveByUserId(
                userId,
                SessionStatus.ACTIVE,
                SessionStatus.REVOKED
        );
    }

    private void handleRefreshTokenReuse(Session reusedSession) {
        reusedSession.setStatus(SessionStatus.REUSED);

        Session nextSession = reusedSession.getReplacedBy();

        Set<Long> visitedSessionIds = new HashSet<>();

        visitedSessionIds.add(reusedSession.getSessionId());

        while (nextSession != null) {
            Long sessionId = nextSession.getSessionId();

            if (!visitedSessionIds.add(sessionId)) {
                throw new IllegalStateException("Session replacement cycle detected");
            }

            Session lockedSession = sessionRepository
                    .findForUpdateById(sessionId)
                    .orElseThrow(() ->
                            new IllegalStateException("Replacement session not found: " + sessionId)
                    );

            if (lockedSession.getStatus() == SessionStatus.ACTIVE) {
                lockedSession.setStatus(SessionStatus.REVOKED);
            }

            nextSession = lockedSession.getReplacedBy();
        }
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
