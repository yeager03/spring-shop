package com.yeager.shop.catalog.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@AllArgsConstructor
public class ProductDetailsResponse {
    private Long productId;

    private String title;
    private String slug;
    private String description;

    private BigDecimal price;
    private boolean inStock;

    private List<ProductImageResponse> images;
    private List<CategoryResponse> categories;
}
