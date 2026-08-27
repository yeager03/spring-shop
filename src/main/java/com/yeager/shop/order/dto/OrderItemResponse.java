package com.yeager.shop.order.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class OrderItemResponse {
    private Long productId;

    private String productTitle;

    private BigDecimal unitPrice;

    private int quantity;

    private BigDecimal subtotal;
}
