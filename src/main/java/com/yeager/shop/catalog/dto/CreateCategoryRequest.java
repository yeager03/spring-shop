package com.yeager.shop.catalog.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CreateCategoryRequest {
    @NotBlank(message = "{category.create.name.not-blank}")
    @Size(max = 120, message = "{category.create.name.size}")
    private String name;

    @NotBlank(message = "{category.create.slug.not-blank}")
    @Size(max = 255, message = "{category.create.slug.size}")
    @Pattern(
            regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$",
            message = "{category.create.slug.invalid}"
    )
    private String slug;

    @Positive(message = "{category.create.parent-id.positive}")
    private Long parentId;

    @PositiveOrZero(message = "{category.create.position.positive-or-zero}")
    private Integer position = 0;
}
