package com.yeager.shop.authentication.controller;

import com.yeager.shop.authentication.dto.*;
import com.yeager.shop.authentication.security.AuthenticatedUserPrincipal;
import com.yeager.shop.authentication.security.RefreshTokenCookieService;
import com.yeager.shop.authentication.service.AuthenticationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/authentication")
@RequiredArgsConstructor
public class AuthenticationController {
    private final AuthenticationService authenticationService;
    private final RefreshTokenCookieService refreshTokenCookieService;

    @PostMapping("/sign-up")
    public ResponseEntity<SignUpResponse> signUp(
            @Valid @RequestBody SignUpRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(authenticationService.signUp(request));
    }

    @PostMapping("/sign-in")
    public ResponseEntity<AccessTokenResponse> signIn(
            @Valid @RequestBody SignInRequest request
    ) {
        AuthenticationResult result = authenticationService.signIn(request);

        ResponseCookie refreshCookie = refreshTokenCookieService.create(result.getRefreshToken());

        return ResponseEntity
                .ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(result.getResponse());
    }

    @PostMapping("/refresh-tokens")
    public ResponseEntity<AccessTokenResponse> refreshTokens(
            @CookieValue(
                    name = "refresh_token",
                    required = false
            )
            String refreshToken
    ) {
        System.out.println("COOKIE = " + refreshToken);

        AuthenticationResult result = authenticationService.refreshTokens(refreshToken);

        ResponseCookie refreshCookie = refreshTokenCookieService.create(result.getRefreshToken());

        return ResponseEntity
                .ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(result.getResponse());
    }

    @PostMapping("/sign-out")
    public ResponseEntity<Void> signOut(
            @CookieValue(
                    name = "refresh_token",
                    required = false
            )
            String refreshToken
    ) {
        authenticationService.signOut(refreshToken);

        ResponseCookie deletedCookie = refreshTokenCookieService.delete();

        return ResponseEntity
                .noContent()
                .header(HttpHeaders.SET_COOKIE, deletedCookie.toString())
                .build();
    }

    @PatchMapping("/password")
    public ResponseEntity<Void> changePassword(
            @AuthenticationPrincipal
            AuthenticatedUserPrincipal principal,

            @Valid
            @RequestBody
            ChangePasswordRequest request
    ) {
        authenticationService.changePassword(principal.getUserId(), request);

        ResponseCookie deletedCookie = refreshTokenCookieService.delete();

        return ResponseEntity
                .noContent()
                .header(HttpHeaders.SET_COOKIE, deletedCookie.toString())
                .build();
    }
}
