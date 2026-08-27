package com.yeager.shop.order.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class OrderListQuery {
    @Min(value = 1, message = "{order.list.page.min}")
    private Integer page = 1;

    @Min(value = 1, message = "{order.list.limit.min}")
    @Max(value = 20, message = "{order.list.limit.max}")
    private Integer limit = 20;
}
