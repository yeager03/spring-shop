package com.yeager.shop.catalog.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CategoryDetailsResponse {
    private Long categoryId;
    private String name;
    private String slug;
    private Integer position;

    private CategoryResponse parent;
}
