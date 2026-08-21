package com.yeager.shop.catalog.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UpdateProductImageRequest {
    @NotNull(message = "{product.image.update.position.not-null}")
    @PositiveOrZero(message = "{product.image.update.position.positive-or-zero}")
    private Integer position;
}
