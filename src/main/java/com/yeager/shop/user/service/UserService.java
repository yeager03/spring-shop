package com.yeager.shop.user.service;

import com.yeager.shop.common.exception.InvalidOperationException;
import com.yeager.shop.common.exception.ResourceNotFoundException;
import com.yeager.shop.common.exception.StorageException;
import com.yeager.shop.common.storage.StorageService;
import com.yeager.shop.user.dto.CurrentUserResponse;
import com.yeager.shop.user.dto.UpdateAvatarRequest;
import com.yeager.shop.user.dto.UpdateCurrentUserRequest;
import com.yeager.shop.user.dto.UserAvatarResponse;
import com.yeager.shop.user.entity.User;
import com.yeager.shop.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {
    private static final long MAX_AVATAR_SIZE = 5 * 1024 * 1024;

    private final UserRepository userRepository;

    private final StorageService storageService;

    @Transactional(readOnly = true)
    public CurrentUserResponse getCurrentUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found by id: " + userId)
                );

        return toCurrentUserResponse(user);
    }

    @Transactional
    public CurrentUserResponse updateCurrentUser(Long userId, UpdateCurrentUserRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found by id: " + userId)
                );

        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName().trim());
        }

        if (request.getLastName() != null) {
            user.setLastName(request.getLastName().trim());
        }

        return toCurrentUserResponse(user);
    }

    @Transactional
    public UserAvatarResponse updateAvatar(Long userId, UpdateAvatarRequest request) {
        User user = userRepository
                .findForUpdateById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found by id: " + userId)
                );

        MultipartFile file = request.getFile();

        validateAvatar(file);

        String extension = resolveAvatarExtension(file);

        String newAvatarKey = generateAvatarKey(userId, extension);

        String oldAvatarKey = user.getAvatarKey();

        storageService.upload(newAvatarKey, file);

        try {
            user.setAvatarKey(newAvatarKey);

            userRepository.flush();
        } catch (DataAccessException exception) {
            try {
                storageService.delete(newAvatarKey);
            } catch (StorageException cleanupException) {
                exception.addSuppressed(cleanupException);
            }

            throw exception;
        }

        if (oldAvatarKey != null) {
            storageService.delete(oldAvatarKey);
        }

        return new UserAvatarResponse(
                storageService.getPublicUrl(newAvatarKey)
        );
    }

    @Transactional
    public void deleteAvatar(Long userId) {
        User user = userRepository
                .findForUpdateById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found by id: " + userId)
                );

        String avatarKey = user.getAvatarKey();

        if (avatarKey == null) {
            return;
        }

        user.setAvatarKey(null);

        userRepository.flush();

        storageService.delete(avatarKey);
    }

    private void validateAvatar(MultipartFile file) {
        if (file.isEmpty()) {
            throw new InvalidOperationException(
                    "Avatar image must not be empty"
            );
        }

        if (file.getSize() > MAX_AVATAR_SIZE) {
            throw new InvalidOperationException(
                    "Avatar image must not exceed 5 MB"
            );
        }
    }

    private String resolveAvatarExtension(MultipartFile file) {
        String contentType = file.getContentType();

        if (contentType == null) {
            throw new InvalidOperationException("Avatar image content type is missing");
        }

        return switch (contentType) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";

            default -> throw new InvalidOperationException("Avatar image must be JPEG, PNG or WEBP");
        };
    }

    private String generateAvatarKey(Long userId, String extension) {
        return "users/"
                + userId
                + "/avatars/"
                + UUID.randomUUID()
                + extension;
    }

    private CurrentUserResponse toCurrentUserResponse(User user) {
        String avatarUrl = user.getAvatarKey() == null
                ? null
                : storageService.getPublicUrl(user.getAvatarKey());

        return new CurrentUserResponse(
                user.getUserId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole(),
                avatarUrl
        );
    }
}
