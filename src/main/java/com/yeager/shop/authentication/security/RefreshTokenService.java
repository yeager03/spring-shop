package com.yeager.shop.authentication.security;

import com.yeager.shop.common.exception.InvalidCredentialsException;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class RefreshTokenService {
    private static final int SECRET_SIZE = 32;

    private final SecureRandom secureRandom = new SecureRandom();

    public String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));

            return HexFormat
                    .of()
                    .formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    public GeneratedRefreshToken generate() {
        String jti = UUID.randomUUID().toString();

        byte[] secretBytes = new byte[SECRET_SIZE];

        secureRandom.nextBytes(secretBytes);

        String secret = Base64
                .getUrlEncoder()
                .withoutPadding()
                .encodeToString(secretBytes);

        String token = jti + "." + secret;

        String tokenHash = hash(token);

        return new GeneratedRefreshToken(
                jti,
                token,
                tokenHash
        );
    }

    public String extractJti(String token) {
        if (token == null || token.isBlank()) {
            throw new InvalidCredentialsException();
        }

        int separatorIndex = token.indexOf('.');

        if (separatorIndex <= 0
                || separatorIndex == token.length() - 1
                || token.indexOf('.', separatorIndex + 1) != -1) {
            throw new InvalidCredentialsException();
        }

        String jti = token.substring(0, separatorIndex);

        try {
            UUID.fromString(jti);
        } catch (IllegalArgumentException exception) {
            throw new InvalidCredentialsException();
        }

        return jti;
    }

    private byte[] digest(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            return digest.digest(token.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    public boolean matches(String token, String expectedHash) {
        try {
            byte[] actual = digest(token);

            byte[] expected = HexFormat
                    .of()
                    .parseHex(expectedHash);

            return MessageDigest.isEqual(
                    actual,
                    expected
            );
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }
}
