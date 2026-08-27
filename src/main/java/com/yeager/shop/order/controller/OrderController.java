package com.yeager.shop.order.controller;

import com.yeager.shop.authentication.security.AuthenticatedUserPrincipal;
import com.yeager.shop.common.dto.PagedResponse;
import com.yeager.shop.order.dto.CreateOrderRequest;
import com.yeager.shop.order.dto.OrderDetailsResponse;
import com.yeager.shop.order.dto.OrderListQuery;
import com.yeager.shop.order.dto.OrderResponse;
import com.yeager.shop.order.service.OrderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderDetailsResponse> checkout(
            @AuthenticationPrincipal
            AuthenticatedUserPrincipal principal,

            @Valid
            @RequestBody
            CreateOrderRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(orderService.checkout(principal.getUserId(), request));
    }

    @GetMapping
    public ResponseEntity<PagedResponse<OrderResponse>> getOrders(
            @AuthenticationPrincipal
            AuthenticatedUserPrincipal principal,

            @Valid
            @ModelAttribute
            OrderListQuery query
    ) {
        return ResponseEntity.ok(orderService.getOrders(principal.getUserId(), query));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderDetailsResponse> getOrder(
            @AuthenticationPrincipal
            AuthenticatedUserPrincipal principal,

            @PathVariable
            @Positive(message = "{order.common.id.positive}")
            Long orderId
    ) {
        return ResponseEntity.ok(orderService.getOrder(principal.getUserId(), orderId));
    }

    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<Void> cancelOrder(
            @AuthenticationPrincipal
            AuthenticatedUserPrincipal principal,

            @PathVariable
            @Positive(message = "{order.common.id.positive}")
            Long orderId
    ) {
        orderService.cancelOrder(principal.getUserId(), orderId);

        return ResponseEntity
                .noContent()
                .build();
    }
}
