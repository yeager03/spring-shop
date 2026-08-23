package com.yeager.shop.user.dto;

import com.yeager.shop.user.entity.UserRole;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
public class CurrentUserResponse {
    private Long userId;
    private String email;
    private String firstName;
    private String lastName;
    private UserRole role;
    private String avatarUrl;
}
