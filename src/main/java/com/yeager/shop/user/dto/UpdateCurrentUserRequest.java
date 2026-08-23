package com.yeager.shop.user.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UpdateCurrentUserRequest {
    @Size(max = 100, message = "{user.update.first-name.size}")
    @Pattern(regexp = ".*\\S.*", message = "{user.update.first-name.not-blank}")
    private String firstName;

    @Size(max = 100, message = "{user.update.last-name.size}")
    @Pattern(regexp = ".*\\S.*", message = "{user.update.last-name.not-blank}")
    private String lastName;
}
