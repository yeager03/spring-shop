package com.yeager.shop.catalog.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UpdateCategoryRequest {
    @Size(max = 120, message = "{category.update.name.size}")
    private String name;

    @Size(max = 255, message = "{category.update.slug.size}")
    @Pattern(
            regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$",
            message = "{category.update.slug.invalid}"
    )
    private String slug;

    @Setter(AccessLevel.NONE)
    @Positive(message = "{category.update.parent-id.positive}")
    private Long parentId;

    @PositiveOrZero(message = "{category.update.position.positive-or-zero}")
    private Integer position;

    private Boolean active;

    private boolean parentIdProvided;

    public void setParentId(Long parentId) {
        this.parentId = parentId;
        this.parentIdProvided = true;
    }
}
