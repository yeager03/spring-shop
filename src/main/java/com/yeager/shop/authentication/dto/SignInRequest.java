package com.yeager.shop.authentication.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SignInRequest {
    @NotBlank(message = "{authentication.sign-in.email.not-blank}")
    @Email(message = "{authentication.sign-in.email.invalid}")
    @Size(max = 255, message = "{authentication.sign-in.email.size}")
    private String email;

    @NotBlank(message = "{authentication.sign-in.password.not-blank}")
    @Size(max = 100, message = "{authentication.sign-in.password.size}")
    private String password;
}
