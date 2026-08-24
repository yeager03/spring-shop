package com.yeager.shop.cart.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@AllArgsConstructor
public class CartResponse {
    private List<CartItemResponse> items;

    private int totalQuantity;

    private BigDecimal totalAmount;
}
