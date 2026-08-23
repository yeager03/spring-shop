package com.yeager.shop.authentication.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AccessTokenResponse  {
    private String accessToken;

    private String tokenType;

    private long expiresIn;
}
