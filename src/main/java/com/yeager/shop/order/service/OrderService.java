package com.yeager.shop.order.service;

import com.yeager.shop.cart.entity.Cart;
import com.yeager.shop.cart.entity.CartItem;
import com.yeager.shop.cart.repository.CartItemRepository;
import com.yeager.shop.cart.repository.CartRepository;
import com.yeager.shop.catalog.entity.Product;
import com.yeager.shop.catalog.repository.ProductRepository;
import com.yeager.shop.common.dto.PageMeta;
import com.yeager.shop.common.dto.PagedResponse;
import com.yeager.shop.common.exception.InvalidOperationException;
import com.yeager.shop.common.exception.ResourceNotFoundException;
import com.yeager.shop.order.dto.CreateOrderRequest;
import com.yeager.shop.order.dto.OrderDetailsResponse;
import com.yeager.shop.order.dto.OrderItemResponse;
import com.yeager.shop.order.dto.OrderListQuery;
import com.yeager.shop.order.dto.OrderResponse;
import com.yeager.shop.order.entity.Order;
import com.yeager.shop.order.entity.OrderItem;
import com.yeager.shop.order.entity.OrderStatus;
import com.yeager.shop.order.repository.OrderItemRepository;
import com.yeager.shop.order.repository.OrderRepository;
import com.yeager.shop.user.entity.User;
import com.yeager.shop.user.repository.UserRepository;
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
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Transactional
    public OrderDetailsResponse checkout(Long userId, CreateOrderRequest request) {
        User user = userRepository.findForUpdateById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found by id: " + userId)
                );

        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() ->
                        new InvalidOperationException("Cart is empty")
                );

        List<CartItem> cartItems = cartItemRepository.findAllByCartId(cart.getCartId());

        if (cartItems.isEmpty()) {
            throw new InvalidOperationException("Cart is empty");
        }

        Map<Long, Product> lockedProducts = lockProducts(
                cartItems
                        .stream()
                        .map(item -> item.getProduct().getProductId())
                        .toList()
        );

        Order order = new Order();

        order.setUser(user);
        order.setStatus(OrderStatus.CREATED);
        order.setRecipientName(request.getRecipientName().trim());
        order.setRecipientPhone(request.getRecipientPhone().trim());
        order.setCountry(request.getCountry().trim());
        order.setCity(request.getCity().trim());
        order.setStreet(request.getStreet().trim());
        order.setHouse(request.getHouse().trim());
        order.setApartment(trimToNull(request.getApartment()));
        order.setPostalCode(trimToNull(request.getPostalCode()));

        BigDecimal totalAmount = BigDecimal.ZERO;

        for (CartItem cartItem : cartItems) {
            Product product = lockedProducts.get(cartItem.getProduct().getProductId());

            if (product == null || !product.isActive()) {
                throw new InvalidOperationException(
                        "Product is not available: " + cartItem.getProduct().getTitle()
                );
            }

            int quantity = cartItem.getQuantity();

            if (product.getStock() < quantity) {
                throw new InvalidOperationException(
                        "Insufficient stock for product: " + product.getTitle()
                );
            }

            product.setStock(product.getStock() - quantity);

            OrderItem orderItem = new OrderItem();

            orderItem.setProduct(product);
            orderItem.setProductTitle(product.getTitle());
            orderItem.setUnitPrice(product.getPrice());
            orderItem.setQuantity(quantity);

            order.addItem(orderItem);

            totalAmount = totalAmount.add(
                    product.getPrice().multiply(BigDecimal.valueOf(quantity))
            );
        }

        order.setTotalAmount(totalAmount);

        Order savedOrder = orderRepository.save(order);

        cartItemRepository.deleteAllItems(cart.getCartId());

        return toDetailsResponse(savedOrder);
    }

    @Transactional(readOnly = true)
    public PagedResponse<OrderResponse> getOrders(Long userId, OrderListQuery query) {
        Pageable pageable = PageRequest.of(
                query.getPage() - 1,
                query.getLimit(),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<Order> page = orderRepository.findPageByUserId(userId, pageable);

        List<OrderResponse> items = page
                .getContent()
                .stream()
                .map(this::toResponse)
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

        return new PagedResponse<>(items, pageMeta, appliedQuery);
    }

    @Transactional(readOnly = true)
    public OrderDetailsResponse getOrder(Long userId, Long orderId) {
        Order order = orderRepository.findDetailsByIdAndUserId(orderId, userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Order not found by id: " + orderId)
                );

        return toDetailsResponse(order);
    }

    @Transactional
    public void cancelOrder(Long userId, Long orderId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Order not found by id: " + orderId)
                );

        OrderStatus status = order.getStatus();

        if (status != OrderStatus.CREATED && status != OrderStatus.PAID) {
            throw new InvalidOperationException(
                    "Order cannot be cancelled in status " + status
            );
        }

        order.setStatus(OrderStatus.CANCELLED);

        List<OrderItem> items = orderItemRepository.findByOrderIdWithProduct(orderId);

        List<Long> productIds = items
                .stream()
                .map(OrderItem::getProduct)
                .filter(Objects::nonNull)
                .map(Product::getProductId)
                .toList();

        if (productIds.isEmpty()) {
            return;
        }

        Map<Long, Product> lockedProducts = lockProducts(productIds);

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

    private Map<Long, Product> lockProducts(List<Long> productIds) {
        List<Long> sortedIds = productIds
                .stream()
                .distinct()
                .sorted()
                .toList();

        return productRepository.findForUpdateByIds(sortedIds)
                .stream()
                .collect(Collectors.toMap(Product::getProductId, Function.identity()));
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();

        return trimmed.isEmpty() ? null : trimmed;
    }

    private OrderResponse toResponse(Order order) {
        List<OrderItem> items = order.getItems();

        int totalQuantity = items
                .stream()
                .mapToInt(OrderItem::getQuantity)
                .sum();

        return new OrderResponse(
                order.getOrderId(),
                order.getStatus(),
                order.getTotalAmount(),
                totalQuantity,
                items.size(),
                order.getCreatedAt()
        );
    }

    private OrderDetailsResponse toDetailsResponse(Order order) {
        List<OrderItem> items = order.getItems();

        List<OrderItemResponse> itemResponses = items
                .stream()
                .map(this::toItemResponse)
                .toList();

        int totalQuantity = items
                .stream()
                .mapToInt(OrderItem::getQuantity)
                .sum();

        return new OrderDetailsResponse(
                order.getOrderId(),
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
