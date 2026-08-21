package com.yeager.shop.catalog.controller;

import com.yeager.shop.catalog.dto.*;
import com.yeager.shop.catalog.service.ProductService;
import com.yeager.shop.common.dto.PagedResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {
    private final ProductService productService;

    @GetMapping
    public ResponseEntity<PagedResponse<ProductListItemResponse>> getProducts(
            @Valid @ModelAttribute ProductListQuery query
    ) {
        return ResponseEntity.ok(productService.getProducts(query));
    }

    @GetMapping("/{slug}")
    public ResponseEntity<ProductDetailsResponse> getProduct(
            @PathVariable
            @Size(
                    min = 1,
                    max = 255,
                    message = "{product.common.slug.size}"
            )
            @Pattern(
                    regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$",
                    message = "{product.common.slug.invalid}"
            )
            String slug
    ) {
        return ResponseEntity.ok(productService.getProduct(slug));
    }

    @PostMapping
    public ResponseEntity<ProductManagementResponse> createProduct(
            @Valid @RequestBody CreateProductRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(productService.createProduct(request));
    }

    @PatchMapping("/{productId}")
    public ResponseEntity<ProductManagementResponse> updateProduct(
            @PathVariable
            @Positive(message = "{product.common.id.positive}")
            Long productId,

            @Valid
            @RequestBody
            UpdateProductRequest request
    ) {
        return ResponseEntity.ok(productService.updateProduct(productId, request));
    }

    @PutMapping("/{productId}/categories/{categoryId}")
    public ResponseEntity<Void> addCategory(
            @PathVariable
            @Positive(message = "{product.common.id.positive}")
            Long productId,

            @PathVariable
            @Positive(message = "{category.common.id.positive}")
            Long categoryId
    ) {
        productService.addCategory(productId, categoryId);

        return ResponseEntity
                .noContent()
                .build();
    }

    @DeleteMapping("/{productId}/categories/{categoryId}")
    public ResponseEntity<Void> removeCategory(
            @PathVariable
            @Positive(message = "{product.common.id.positive}")
            Long productId,

            @PathVariable
            @Positive(message = "{category.common.id.positive}")
            Long categoryId
    ) {
        productService.removeCategory(productId, categoryId);

        return ResponseEntity
                .noContent()
                .build();
    }

    @PostMapping(
            path = "/{productId}/images",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<ProductImageResponse> addImage(
            @PathVariable
            @Positive(message = "{product.common.id.positive}")
            Long productId,

            @Valid
            @ModelAttribute
            ProductImageUploadRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(productService.addImage(productId, request));
    }

    @DeleteMapping("/{productId}/images/{imageId}")
    public ResponseEntity<Void> deleteImage(
            @PathVariable
            @Positive(message = "{product.common.id.positive}")
            Long productId,

            @PathVariable
            @Positive(message = "{product.image.id.positive}")
            Long imageId
    ) {
        productService.deleteImage(productId, imageId);

        return ResponseEntity
                .noContent()
                .build();
    }

    @PatchMapping("/{productId}/images/{imageId}")
    public ResponseEntity<ProductImageResponse> updateImage(
            @PathVariable
            @Positive(message = "{product.common.id.positive}")
            Long productId,

            @PathVariable
            @Positive(message = "{product.image.id.positive}")
            Long imageId,

            @Valid
            @RequestBody
            UpdateProductImageRequest request
    ) {
        return ResponseEntity.ok(
                productService.updateImagePosition(
                        productId,
                        imageId,
                        request
                )
        );
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<Void> deactivateProduct(
            @PathVariable
            @Positive(message = "{product.common.id.positive}")
            Long productId
    ) {
        productService.deactivateProduct(productId);

        return ResponseEntity
                .noContent()
                .build();
    }
}
