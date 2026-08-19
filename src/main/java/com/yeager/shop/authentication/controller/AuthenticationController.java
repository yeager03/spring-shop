package com.yeager.shop.authentication.controller;

import com.yeager.shop.authentication.dto.SignInRequest;
import com.yeager.shop.authentication.dto.SignInResponse;
import com.yeager.shop.authentication.service.AuthenticationService;
import com.yeager.shop.authentication.dto.SignUpRequest;
import com.yeager.shop.authentication.dto.SignUpResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/authentication")
@RequiredArgsConstructor
public class AuthenticationController {
    private final AuthenticationService authenticationService;

    @PostMapping("/sign-up")
    public ResponseEntity<SignUpResponse> signUp(
            @Valid @RequestBody SignUpRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(authenticationService.signUp(request));
    }

    @PostMapping("/sign-in")
    public ResponseEntity<SignInResponse> signIn(
            @Valid @RequestBody SignInRequest request
    ) {
        return ResponseEntity.ok(authenticationService.signIn(request));
    }
}
