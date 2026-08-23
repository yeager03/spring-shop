package com.yeager.shop.authentication.security;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RefreshTokenCookieService {
    private final JwtProperties jwtProperties;
    private final RefreshCookieProperties cookieProperties;

    public ResponseCookie create(String refreshToken) {
        return ResponseCookie
                .from(cookieProperties.getName(), refreshToken)
                .httpOnly(true)
                .secure(cookieProperties.isSecure())
                .sameSite(cookieProperties.getSameSite())
                .path(cookieProperties.getPath())
                .maxAge(jwtProperties.getRefreshTokenTtl())
                .build();
    }

    public ResponseCookie delete() {
        return ResponseCookie
                .from(cookieProperties.getName(), "")
                .httpOnly(true)
                .secure(cookieProperties.isSecure())
                .sameSite(cookieProperties.getSameSite())
                .path(cookieProperties.getPath())
                .maxAge(0)
                .build();
    }
}
