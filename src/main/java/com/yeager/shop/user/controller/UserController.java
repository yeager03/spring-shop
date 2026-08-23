package com.yeager.shop.user.controller;

import com.yeager.shop.authentication.security.AuthenticatedUserPrincipal;
import com.yeager.shop.user.dto.CurrentUserResponse;
import com.yeager.shop.user.dto.UpdateAvatarRequest;
import com.yeager.shop.user.dto.UpdateCurrentUserRequest;
import com.yeager.shop.user.dto.UserAvatarResponse;
import com.yeager.shop.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<CurrentUserResponse> getCurrentUser(
            @AuthenticationPrincipal
            AuthenticatedUserPrincipal principal
    ) {
        return ResponseEntity.ok(userService.getCurrentUser(principal.getUserId()));
    }

    @PatchMapping("/me")
    public ResponseEntity<CurrentUserResponse> updateCurrentUser(
            @AuthenticationPrincipal
            AuthenticatedUserPrincipal principal,

            @Valid
            @RequestBody
            UpdateCurrentUserRequest request
    ) {
        return ResponseEntity.ok(
                userService.updateCurrentUser(
                        principal.getUserId(),
                        request
                )
        );
    }

    @PutMapping(
            path = "/me/avatar",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<UserAvatarResponse> updateAvatar(
            @AuthenticationPrincipal
            AuthenticatedUserPrincipal principal,

            @Valid
            @ModelAttribute
            UpdateAvatarRequest request
    ) {
        return ResponseEntity.ok(
                userService.updateAvatar(
                        principal.getUserId(),
                        request
                )
        );
    }

    @DeleteMapping("/me/avatar")
    public ResponseEntity<Void> deleteAvatar(
            @AuthenticationPrincipal
            AuthenticatedUserPrincipal principal
    ) {
        userService.deleteAvatar(principal.getUserId());

        return ResponseEntity
                .noContent()
                .build();
    }
}
