package com.yeager.shop.catalog.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ProductSort {
    TITLE("title"),
    PRICE("price"),
    CREATED_AT("createdAt");

    private final String property;
}
