package com.yeager.shop.catalog.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
@NoArgsConstructor
public class ProductImageUploadRequest {
    @NotNull(message = "{product.image.file.not-null}")
    private MultipartFile file;

    @NotNull(message = "{product.image.position.not-null}")
    @PositiveOrZero(message = "{product.image.position.positive-or-zero}")
    private Integer position;
}
