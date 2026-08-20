package com.yeager.shop.catalog.controller;

import com.yeager.shop.catalog.dto.*;
import com.yeager.shop.catalog.service.CategoryService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
public class CategoryController {
    private final CategoryService categoryService;

    @PostMapping
    public ResponseEntity<CategoryResponse> createCategory(
            @Valid @RequestBody CreateCategoryRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(categoryService.createCategory(request));
    }

    @GetMapping
    public ResponseEntity<List<CategoryTreeResponse>> getCategories() {
        return ResponseEntity.ok(categoryService.getCategories());
    }

    @GetMapping("/{slug}")
    public ResponseEntity<CategoryDetailsResponse> getCategory(
            @PathVariable
            @Size(
                    min = 1,
                    max = 255,
                    message = "{category.common.slug.size}"
            )
            @Pattern(
                    regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$",
                    message = "{category.common.slug.invalid}"
            )
            String slug
    ) {
        return ResponseEntity.ok(categoryService.getCategory(slug));
    }


    @PatchMapping("/{categoryId}")
    public ResponseEntity<CategoryResponse> updateCategory(
            @PathVariable
            @Positive(message = "{category.common.id.positive}")
            Long categoryId,

            @Valid
            @RequestBody
            UpdateCategoryRequest request
    ) {
        return ResponseEntity.ok(categoryService.updateCategory(categoryId, request));
    }

    @DeleteMapping("/{categoryId}")
    public ResponseEntity<Void> deactivateCategory(
            @PathVariable
            @Positive(message = "{category.common.id.positive}")
            Long categoryId
    ) {
        categoryService.deactivateCategory(categoryId);

        return ResponseEntity
                .noContent()
                .build();
    }
}
