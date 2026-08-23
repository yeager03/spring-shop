package com.yeager.shop.authentication.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "security.refresh-cookie")
public class RefreshCookieProperties {
    private String name = "refresh_token";

    private boolean secure = true;

    private String sameSite = "Strict";

    private String path = "/";
}
