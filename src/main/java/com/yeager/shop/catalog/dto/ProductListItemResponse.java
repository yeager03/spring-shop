package com.yeager.shop.catalog.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class ProductListItemResponse {
    private Long productId;

    private String title;
    private String slug;

    private BigDecimal price;
    private boolean inStock;

    private String mainImageUrl;
}
