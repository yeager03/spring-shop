package com.yeager.shop.order.dto;

import com.yeager.shop.order.entity.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Getter
@AllArgsConstructor
public class OrderDetailsResponse {
    private Long orderId;

    private OrderStatus status;

    private BigDecimal totalAmount;

    private int totalQuantity;

    private String recipientName;

    private String recipientPhone;

    private String country;

    private String city;

    private String street;

    private String house;

    private String apartment;

    private String postalCode;

    private List<OrderItemResponse> items;

    private Instant createdAt;

    private Instant updatedAt;
}
