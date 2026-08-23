package com.yeager.shop.user.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
@NoArgsConstructor
public class UpdateAvatarRequest {
    @NotNull(message = "{user.avatar.file.not-null}")
    private MultipartFile file;
}
