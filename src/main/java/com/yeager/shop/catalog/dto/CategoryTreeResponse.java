package com.yeager.shop.catalog.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
@AllArgsConstructor
public class CategoryTreeResponse {
    private final Long categoryId;
    private final String name;
    private final String slug;

    private final List<CategoryTreeResponse> children = new ArrayList<>();

    public void addChild(CategoryTreeResponse child) {
        children.add(child);
    }
}
