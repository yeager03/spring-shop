package com.yeager.shop.common.exception;

public class RefreshTokenReuseException extends RuntimeException {
    public RefreshTokenReuseException() {
        super("Invalid refresh token");
    }
}
