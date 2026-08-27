package com.yeager.shop.order.dto;

import com.yeager.shop.order.entity.OrderStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UpdateOrderStatusRequest {
    @NotNull(message = "{order.management.status.not-null}")
    private OrderStatus status;
}
