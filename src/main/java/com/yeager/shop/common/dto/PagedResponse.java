package com.yeager.shop.common.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
@AllArgsConstructor
public class PagedResponse<T> {
    private List<T> items;

    private PageMeta pageMeta;

    private Map<String, Object> appliedQuery;
}
