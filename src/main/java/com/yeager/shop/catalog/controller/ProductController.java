package com.yeager.shop.catalog.controller;

import com.yeager.shop.catalog.dto.ProductDetailsResponse;
import com.yeager.shop.catalog.dto.ProductListItemResponse;
import com.yeager.shop.catalog.dto.ProductListQuery;
import com.yeager.shop.catalog.service.ProductService;
import com.yeager.shop.common.dto.PagedResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {
    private final ProductService productService;

    @GetMapping
    public PagedResponse<ProductListItemResponse> getProducts(
            @Valid @ModelAttribute ProductListQuery query
    ) {
        return productService.getProducts(query);
    }

    @GetMapping("/{slug}")
    public ProductDetailsResponse getProduct(
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
        return productService.getProduct(slug);
    }
}
