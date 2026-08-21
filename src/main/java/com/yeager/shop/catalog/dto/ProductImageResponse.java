package com.yeager.shop.catalog.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ProductImageResponse {
    private Long imageId;
    private String imageKey;
    private String imageUrl;
    private int position;
}
