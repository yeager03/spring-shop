package com.yeager.shop.authentication.security;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class GeneratedRefreshToken {
    private final String jti;
    private final String token;
    private final String tokenHash;
}
