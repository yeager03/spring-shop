package com.yeager.shop.cart.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class CartItemResponse {
    private Long productId;

    private String title;

    private String slug;

    private BigDecimal unitPrice;

    private int quantity;

    private BigDecimal subtotal;

    private int availableQuantity;

    private boolean available;
}
