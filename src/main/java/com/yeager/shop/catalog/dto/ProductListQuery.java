package com.yeager.shop.catalog.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class ProductListQuery {
    @Min(value = 1, message = "{product.list.page.min}")
    private Integer page = 1;

    @Min(value = 1, message = "{product.list.limit.min}")
    @Max(value = 20, message = "{product.list.limit.max}")
    private Integer limit = 20;

    @Size(max = 200, message = "{product.list.search.size}")
    private String search;

    private ProductSort sort = ProductSort.CREATED_AT;

    private SortDirection order = SortDirection.DESC;

    @Size(max = 3, message = "{product.list.categories.size}")
    private List<
            @NotNull(message = "{product.list.category.not-null}")
            @Positive(message = "{product.list.category.positive}")
                    Long
            > categoryIds = new ArrayList<>();
}
