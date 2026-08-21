package com.yeager.shop.catalog.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class CreateProductRequest {
    @NotBlank(message = "{product.create.title.not-blank}")
    @Size(max = 255, message = "{product.create.title.size}")
    private String title;

    @NotBlank(message = "{product.create.slug.not-blank}")
    @Size(max = 255, message = "{product.create.slug.size}")
    @Pattern(
            regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$",
            message = "{product.create.slug.invalid}"
    )
    private String slug;

    private String description;

    @NotNull(message = "{product.create.price.not-null}")
    @DecimalMin(
            value = "0.00",
            message = "{product.create.price.min}"
    )
    @Digits(
            integer = 10,
            fraction = 2,
            message = "{product.create.price.digits}"
    )
    private BigDecimal price;

    @NotNull(message = "{product.create.stock.not-null}")
    @PositiveOrZero(message = "{product.create.stock.positive-or-zero}")
    private Integer stock = 0;
}
