package com.yeager.shop.order.controller;

import com.yeager.shop.common.dto.PagedResponse;
import com.yeager.shop.order.dto.ManagedOrderDetailsResponse;
import com.yeager.shop.order.dto.ManagedOrderResponse;
import com.yeager.shop.order.dto.OrderManagementListQuery;
import com.yeager.shop.order.dto.UpdateOrderStatusRequest;
import com.yeager.shop.order.service.OrderManagementService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/management/orders")
@RequiredArgsConstructor
public class OrderManagementController {
    private final OrderManagementService orderManagementService;

    @GetMapping
    public ResponseEntity<PagedResponse<ManagedOrderResponse>> getOrders(
            @Valid
            @ModelAttribute
            OrderManagementListQuery query
    ) {
        return ResponseEntity.ok(orderManagementService.getOrders(query));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<ManagedOrderDetailsResponse> getOrder(
            @PathVariable
            @Positive(message = "{order.common.id.positive}")
            Long orderId
    ) {
        return ResponseEntity.ok(orderManagementService.getOrder(orderId));
    }

    @PatchMapping("/{orderId}/status")
    public ResponseEntity<ManagedOrderDetailsResponse> updateStatus(
            @PathVariable
            @Positive(message = "{order.common.id.positive}")
            Long orderId,

            @Valid
            @RequestBody
            UpdateOrderStatusRequest request
    ) {
        return ResponseEntity.ok(orderManagementService.updateStatus(orderId, request));
    }
}
