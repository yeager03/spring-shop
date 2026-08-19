package com.yeager.shop.user.service;

import com.yeager.shop.common.exception.ResourceNotFoundException;
import com.yeager.shop.user.dto.CurrentUserResponse;
import com.yeager.shop.user.entity.User;
import com.yeager.shop.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public CurrentUserResponse getCurrentUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found by id: " + userId)
                );

        return new CurrentUserResponse(
                user.getUserId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole(),
                user.getAvatarKey()
        );
    }
}
