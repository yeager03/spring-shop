package com.yeager.shop.common.config;

import com.yeager.shop.common.security.RestAccessDeniedHandler;
import com.yeager.shop.common.security.RestAuthenticationEntryPoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationConverter jwtAuthenticationConverter,
            RestAuthenticationEntryPoint authenticationEntryPoint,
            RestAccessDeniedHandler accessDeniedHandler
    ) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(authorizeRequests ->
                        authorizeRequests
                                .requestMatchers(
                                        HttpMethod.GET,
                                        "/products",
                                        "/products/**",
                                        "/categories",
                                        "/categories/**"
                                ).permitAll()

                                .requestMatchers(
                                        HttpMethod.POST,
                                        "/products",
                                        "/products/**",
                                        "/categories"
                                ).hasAnyRole("MANAGER", "ADMIN")

                                .requestMatchers(
                                        HttpMethod.PUT,
                                        "/products/**"
                                ).hasAnyRole("MANAGER", "ADMIN")

                                .requestMatchers(
                                        HttpMethod.PATCH,
                                        "/products/**",
                                        "/categories/**"
                                ).hasAnyRole("MANAGER", "ADMIN")

                                .requestMatchers(
                                        HttpMethod.DELETE,
                                        "/products/**",
                                        "/categories/**"
                                ).hasAnyRole("MANAGER", "ADMIN")

                                .requestMatchers(
                                        HttpMethod.POST,
                                        "/authentication/sign-up",
                                        "/authentication/sign-in"
                                ).permitAll()

                                .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)


                        .jwt(jwt -> jwt
                                .jwtAuthenticationConverter(
                                        jwtAuthenticationConverter
                                )
                        )
                );

        return http.build();
    }
}
