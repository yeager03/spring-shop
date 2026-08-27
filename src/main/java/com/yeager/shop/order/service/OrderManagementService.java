package com.yeager.shop.order.service;

import com.yeager.shop.catalog.entity.Product;
import com.yeager.shop.catalog.repository.ProductRepository;
import com.yeager.shop.common.dto.PageMeta;
import com.yeager.shop.common.dto.PagedResponse;
import com.yeager.shop.common.exception.InvalidOperationException;
import com.yeager.shop.common.exception.ResourceNotFoundException;
import com.yeager.shop.order.dto.ManagedOrderDetailsResponse;
import com.yeager.shop.order.dto.ManagedOrderResponse;
import com.yeager.shop.order.dto.OrderItemResponse;
import com.yeager.shop.order.dto.OrderManagementListQuery;
import com.yeager.shop.order.dto.UpdateOrderStatusRequest;
import com.yeager.shop.order.entity.Order;
import com.yeager.shop.order.entity.OrderItem;
import com.yeager.shop.order.entity.OrderStatus;
import com.yeager.shop.order.repository.OrderItemRepository;
import com.yeager.shop.order.repository.OrderRepository;
import com.yeager.shop.order.repository.projection.OrderItemsSummaryProjection;
import com.yeager.shop.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderManagementService {
    private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED_TRANSITIONS = Map.of(
            OrderStatus.CREATED, Set.of(OrderStatus.PAID, OrderStatus.CANCELLED),
            OrderStatus.PAID, Set.of(OrderStatus.PROCESSING, OrderStatus.CANCELLED),
            OrderStatus.PROCESSING, Set.of(OrderStatus.SHIPPED, OrderStatus.CANCELLED),
            OrderStatus.SHIPPED, Set.of(OrderStatus.DELIVERED),
            OrderStatus.DELIVERED, Set.of(),
            OrderStatus.CANCELLED, Set.of()
    );

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public PagedResponse<ManagedOrderResponse> getOrders(OrderManagementListQuery query) {
        Pageable pageable = PageRequest.of(
                query.getPage() - 1,
                query.getLimit(),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<Order> page = orderRepository.findPageForManagement(
                query.getStatus(),
                query.getUserId(),
                pageable
        );

        List<Long> orderIds = page
                .getContent()
                .stream()
                .map(Order::getOrderId)
                .toList();

        Map<Long, OrderItemsSummaryProjection> summaries = orderIds.isEmpty()
                ? Map.of()
                : orderItemRepository.findItemSummariesByOrderIds(orderIds)
                        .stream()
                        .collect(Collectors.toMap(
                                OrderItemsSummaryProjection::getOrderId,
                                Function.identity()
                        ));

        List<ManagedOrderResponse> items = page
                .getContent()
                .stream()
                .map(order -> toResponse(order, summaries.get(order.getOrderId())))
                .toList();

        PageMeta pageMeta = new PageMeta(
                query.getPage(),
                query.getLimit(),
                page.getTotalPages(),
                page.getTotalElements(),
                page.hasNext()
        );

        Map<String, Object> appliedQuery = new LinkedHashMap<>();

        appliedQuery.put("page", query.getPage());
        appliedQuery.put("limit", query.getLimit());

        if (query.getStatus() != null) {
            appliedQuery.put("status", query.getStatus());
        }

        if (query.getUserId() != null) {
            appliedQuery.put("userId", query.getUserId());
        }

        return new PagedResponse<>(items, pageMeta, appliedQuery);
    }

    @Transactional(readOnly = true)
    public ManagedOrderDetailsResponse getOrder(Long orderId) {
        Order order = orderRepository.findDetailsById(orderId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Order not found by id: " + orderId)
                );

        return toDetailsResponse(order);
    }

    @Transactional
    public ManagedOrderDetailsResponse updateStatus(Long orderId, UpdateOrderStatusRequest request) {
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Order not found by id: " + orderId)
                );

        OrderStatus current = order.getStatus();
        OrderStatus target = request.getStatus();

        if (!canTransition(current, target)) {
            throw new InvalidOperationException(
                    "Cannot change order status from " + current + " to " + target
            );
        }

        if (target == OrderStatus.CANCELLED) {
            restoreStock(orderId);
        }

        order.setStatus(target);

        Order refreshed = orderRepository.findDetailsById(orderId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Order not found by id: " + orderId)
                );

        return toDetailsResponse(refreshed);
    }

    private boolean canTransition(OrderStatus current, OrderStatus target) {
        return ALLOWED_TRANSITIONS.getOrDefault(current, Set.of()).contains(target);
    }

    private void restoreStock(Long orderId) {
        List<OrderItem> items = orderItemRepository.findByOrderIdWithProduct(orderId);

        List<Long> productIds = items
                .stream()
                .map(OrderItem::getProduct)
                .filter(Objects::nonNull)
                .map(Product::getProductId)
                .distinct()
                .sorted()
                .toList();

        if (productIds.isEmpty()) {
            return;
        }

        Map<Long, Product> lockedProducts = productRepository.findForUpdateByIds(productIds)
                .stream()
                .collect(Collectors.toMap(Product::getProductId, Function.identity()));

        for (OrderItem item : items) {
            Product product = item.getProduct();

            if (product == null) {
                continue;
            }

            Product locked = lockedProducts.get(product.getProductId());

            if (locked == null) {
                continue;
            }

            locked.setStock(locked.getStock() + item.getQuantity());
        }
    }

    private ManagedOrderResponse toResponse(Order order, OrderItemsSummaryProjection summary) {
        int totalQuantity = summary != null && summary.getTotalQuantity() != null
                ? summary.getTotalQuantity().intValue()
                : 0;

        int itemCount = summary != null && summary.getItemCount() != null
                ? summary.getItemCount().intValue()
                : 0;

        User user = order.getUser();

        return new ManagedOrderResponse(
                order.getOrderId(),
                user.getUserId(),
                user.getEmail(),
                order.getStatus(),
                order.getTotalAmount(),
                totalQuantity,
                itemCount,
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }

    private ManagedOrderDetailsResponse toDetailsResponse(Order order) {
        List<OrderItem> items = order.getItems();

        List<OrderItemResponse> itemResponses = items
                .stream()
                .map(this::toItemResponse)
                .toList();

        int totalQuantity = items
                .stream()
                .mapToInt(OrderItem::getQuantity)
                .sum();

        User user = order.getUser();

        return new ManagedOrderDetailsResponse(
                order.getOrderId(),
                user.getUserId(),
                user.getEmail(),
                order.getStatus(),
                order.getTotalAmount(),
                totalQuantity,
                order.getRecipientName(),
                order.getRecipientPhone(),
                order.getCountry(),
                order.getCity(),
                order.getStreet(),
                order.getHouse(),
                order.getApartment(),
                order.getPostalCode(),
                itemResponses,
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }

    private OrderItemResponse toItemResponse(OrderItem item) {
        Product product = item.getProduct();

        BigDecimal subtotal = item
                .getUnitPrice()
                .multiply(BigDecimal.valueOf(item.getQuantity()));

        return new OrderItemResponse(
                product != null ? product.getProductId() : null,
                item.getProductTitle(),
                item.getUnitPrice(),
                item.getQuantity(),
                subtotal
        );
    }
}
