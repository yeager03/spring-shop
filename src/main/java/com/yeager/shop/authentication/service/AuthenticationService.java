package com.yeager.shop.authentication.service;

import com.yeager.shop.authentication.dto.SignInRequest;
import com.yeager.shop.authentication.dto.SignInResponse;
import com.yeager.shop.authentication.security.JwtProperties;
import com.yeager.shop.authentication.security.JwtService;
import com.yeager.shop.common.exception.InvalidCredentialsException;
import com.yeager.shop.common.exception.ResourceAlreadyExistsException;
import com.yeager.shop.authentication.dto.SignUpRequest;
import com.yeager.shop.authentication.dto.SignUpResponse;
import com.yeager.shop.user.entity.User;
import com.yeager.shop.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthenticationService {
    private final UserRepository userRepository;
    
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

    @Transactional(readOnly = true)
    public SignInResponse signIn(SignInRequest request) {
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

        return new SignInResponse(
                accessToken,
                "Bearer",
                jwtProperties.getAccessTokenTtl().toSeconds()
        );
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
