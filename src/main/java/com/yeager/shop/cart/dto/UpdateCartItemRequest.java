package com.yeager.shop.cart.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UpdateCartItemRequest {
    @NotNull(message = "{cart.item.quantity.not-null}")
    @Positive(message = "{cart.item.quantity.positive}")
    private Integer quantity;
}
