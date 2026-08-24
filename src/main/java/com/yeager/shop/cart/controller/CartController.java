package com.yeager.shop.cart.controller;

import com.yeager.shop.authentication.security.AuthenticatedUserPrincipal;
import com.yeager.shop.cart.dto.CartItemResponse;
import com.yeager.shop.cart.dto.CartResponse;
import com.yeager.shop.cart.dto.UpdateCartItemRequest;
import com.yeager.shop.cart.service.CartService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {
    private final CartService cartService;

    @GetMapping
    public ResponseEntity<CartResponse> getCart(
            @AuthenticationPrincipal
            AuthenticatedUserPrincipal principal
    ) {
        return ResponseEntity.ok(cartService.getCart(principal.getUserId()));
    }

    @PutMapping("/items/{productId}")
    public ResponseEntity<CartItemResponse> setItem(
            @AuthenticationPrincipal
            AuthenticatedUserPrincipal principal,

            @PathVariable
            @Positive(message = "{cart.item.product-id.positive}")
            Long productId,

            @Valid
            @RequestBody
            UpdateCartItemRequest request
    ) {
        return ResponseEntity.ok(
                cartService.setItem(
                        principal.getUserId(),
                        productId,
                        request
                )
        );
    }

    @DeleteMapping("/items/{productId}")
    public ResponseEntity<Void> deleteItem(
            @AuthenticationPrincipal
            AuthenticatedUserPrincipal principal,

            @PathVariable
            @Positive(message = "{cart.item.product-id.positive}")
            Long productId
    ) {
        cartService.deleteItem(principal.getUserId(), productId);

        return ResponseEntity
                .noContent()
                .build();
    }

    @DeleteMapping
    public ResponseEntity<Void> clearCart(
            @AuthenticationPrincipal
            AuthenticatedUserPrincipal principal
    ) {
        cartService.clearCart(principal.getUserId());

        return ResponseEntity
                .noContent()
                .build();
    }
}
