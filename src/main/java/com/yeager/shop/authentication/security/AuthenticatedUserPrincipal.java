package com.yeager.shop.authentication.security;

import com.yeager.shop.user.entity.UserRole;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;

import java.util.Collection;
import java.util.Map;

@Getter
@RequiredArgsConstructor
public class AuthenticatedUserPrincipal implements OAuth2AuthenticatedPrincipal {
    private final Long userId;
    private final UserRole role;

    private final Map<String, Object> attributes;
    private final Collection<? extends GrantedAuthority> authorities;

    @Override
    public String getName() {
        return userId.toString();
    }
}
