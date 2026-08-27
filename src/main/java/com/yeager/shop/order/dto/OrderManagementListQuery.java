package com.yeager.shop.order.dto;

import com.yeager.shop.order.entity.OrderStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class OrderManagementListQuery {
    @Min(value = 1, message = "{order.management.list.page.min}")
    private Integer page = 1;

    @Min(value = 1, message = "{order.management.list.limit.min}")
    @Max(value = 20, message = "{order.management.list.limit.max}")
    private Integer limit = 20;

    private OrderStatus status;

    @Positive(message = "{order.management.list.user-id.positive}")
    private Long userId;
}
