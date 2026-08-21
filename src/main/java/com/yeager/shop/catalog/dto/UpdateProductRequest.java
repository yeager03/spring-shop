package com.yeager.shop.catalog.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class UpdateProductRequest {
    @Size(max = 255, message = "{product.update.title.size}")
    private String title;

    @Size(max = 255, message = "{product.update.slug.size}")
    @Pattern(
            regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$",
            message = "{product.update.slug.invalid}"
    )
    private String slug;

    @Setter(AccessLevel.NONE)
    private String description;

    @DecimalMin(
            value = "0.00",
            message = "{product.update.price.min}"
    )
    @Digits(
            integer = 10,
            fraction = 2,
            message = "{product.update.price.digits}"
    )
    private BigDecimal price;

    @PositiveOrZero(message = "{product.update.stock.positive-or-zero}")
    private Integer stock;

    private Boolean active;

    @JsonIgnore
    @Setter(AccessLevel.NONE)
    private boolean descriptionProvided;

    public void setDescription(String description) {
        this.description = description;
        this.descriptionProvided = true;
    }
}
