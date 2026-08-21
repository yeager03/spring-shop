package com.yeager.shop.catalog.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class ProductManagementResponse {
    private Long productId;

    private String title;

    private String slug;

    private String description;

    private BigDecimal price;

    private Integer stock;

    private boolean active;
}
