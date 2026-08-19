package com.yeager.shop.authentication.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SignUpRequest {
    @NotBlank(message = "{authentication.sign-up.email.not-blank}")
    @Email(message = "{authentication.sign-up.email.invalid}")
    @Size(max = 255, message = "{authentication.sign-up.email.size}")
    private String email;

    @NotBlank(message = "{authentication.sign-up.password.not-blank}")
    @Size(min = 8, max = 100, message = "{authentication.sign-up.password.size}")
    private String password;

    @NotBlank(message = "{authentication.sign-up.first-name.not-blank}")
    @Size(max = 100, message = "{authentication.sign-up.first-name.size}")
    private String firstName;

    @NotBlank(message = "{authentication.sign-up.last-name.not-blank}")
    @Size(max = 100, message = "{authentication.sign-up.last-name.size}")
    private String lastName;
}
