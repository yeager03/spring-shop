package com.yeager.shop.order.dto;

import com.yeager.shop.order.entity.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@AllArgsConstructor
public class OrderResponse {
    private Long orderId;

    private OrderStatus status;

    private BigDecimal totalAmount;

    private int totalQuantity;

    private int itemCount;

    private Instant createdAt;
}
