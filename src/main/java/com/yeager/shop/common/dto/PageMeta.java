package com.yeager.shop.common.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PageMeta {
    private int page;
    private int limit;
    private int totalPages;
    private long totalElements;
    private boolean hasNext;
}
