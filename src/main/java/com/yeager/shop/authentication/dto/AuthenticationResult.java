package com.yeager.shop.authentication.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AuthenticationResult  {
    private final AccessTokenResponse response;

    private final String refreshToken;
}
