package com.yeager.shop.order.dto;

import com.yeager.shop.order.entity.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@AllArgsConstructor
public class ManagedOrderResponse {
    private Long orderId;

    private Long userId;

    private String userEmail;

    private OrderStatus status;

    private BigDecimal totalAmount;

    private int totalQuantity;

    private int itemCount;

    private Instant createdAt;

    private Instant updatedAt;
}
