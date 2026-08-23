package com.yeager.shop.authentication.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ChangePasswordRequest {
    @NotBlank(message = "{authentication.change-password.current-password.not-blank}")
    @Size(max = 100, message = "{authentication.change-password.current-password.size}")
    private String currentPassword;

    @NotBlank(message = "{authentication.change-password.new-password.not-blank}")
    @Size(min = 8, max = 100, message = "{authentication.change-password.new-password.size}")
    private String newPassword;
}
