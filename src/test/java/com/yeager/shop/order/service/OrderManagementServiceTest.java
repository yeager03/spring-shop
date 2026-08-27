package com.yeager.shop.order.service;

import com.yeager.shop.catalog.entity.Product;
import com.yeager.shop.catalog.repository.ProductRepository;
import com.yeager.shop.common.dto.PagedResponse;
import com.yeager.shop.common.exception.InvalidOperationException;
import com.yeager.shop.common.exception.ResourceNotFoundException;
import com.yeager.shop.order.dto.ManagedOrderDetailsResponse;
import com.yeager.shop.order.dto.ManagedOrderResponse;
import com.yeager.shop.order.dto.OrderManagementListQuery;
import com.yeager.shop.order.dto.UpdateOrderStatusRequest;
import com.yeager.shop.order.entity.Order;
import com.yeager.shop.order.entity.OrderItem;
import com.yeager.shop.order.entity.OrderStatus;
import com.yeager.shop.order.repository.OrderItemRepository;
import com.yeager.shop.order.repository.OrderRepository;
import com.yeager.shop.order.repository.projection.OrderItemsSummaryProjection;
import com.yeager.shop.user.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderManagementServiceTest {
    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private OrderManagementService orderManagementService;

    private User user(long id, String email) {
        User user = new User();

        user.setUserId(id);
        user.setEmail(email);

        return user;
    }

    private Product product(long id, int stock) {
        Product product = new Product();

        product.setProductId(id);
        product.setTitle("Pizza");
        product.setPrice(new BigDecimal("10.00"));
        product.setStock(stock);
        product.setActive(true);

        return product;
    }

    private Order order(long id, OrderStatus status, User user) {
        Order order = new Order();

        order.setOrderId(id);
        order.setUser(user);
        order.setStatus(status);
        order.setTotalAmount(new BigDecimal("20.00"));
        order.setRecipientName("John Doe");
        order.setRecipientPhone("+1000000");
        order.setCountry("Wonderland");
        order.setCity("Capital");
        order.setStreet("Main");
        order.setHouse("1");

        return order;
    }

    private OrderItem orderItem(Product product, int quantity) {
        OrderItem item = new OrderItem();

        item.setProduct(product);
        item.setProductTitle("Pizza");
        item.setUnitPrice(new BigDecimal("10.00"));
        item.setQuantity(quantity);

        return item;
    }

    private UpdateOrderStatusRequest request(OrderStatus status) {
        UpdateOrderStatusRequest request = new UpdateOrderStatusRequest();

        request.setStatus(status);

        return request;
    }

    @Test
    void getOrders_shouldMapRowsAndMergeItemSummaries() {
        User user = user(7L, "buyer@example.com");
        Order order = order(10L, OrderStatus.CREATED, user);

        when(orderRepository.findPageForManagement(isNull(), isNull(), any()))
                .thenReturn(new PageImpl<>(List.of(order), PageRequest.of(0, 20), 1));

        OrderItemsSummaryProjection summary = mock(OrderItemsSummaryProjection.class);
        when(summary.getOrderId()).thenReturn(10L);
        when(summary.getTotalQuantity()).thenReturn(5L);
        when(summary.getItemCount()).thenReturn(2L);
        when(orderItemRepository.findItemSummariesByOrderIds(List.of(10L)))
                .thenReturn(List.of(summary));

        PagedResponse<ManagedOrderResponse> response =
                orderManagementService.getOrders(new OrderManagementListQuery());

        assertEquals(1, response.getItems().size());

        ManagedOrderResponse row = response.getItems().get(0);

        assertEquals(10L, row.getOrderId());
        assertEquals(7L, row.getUserId());
        assertEquals("buyer@example.com", row.getUserEmail());
        assertEquals(OrderStatus.CREATED, row.getStatus());
        assertEquals(5, row.getTotalQuantity());
        assertEquals(2, row.getItemCount());
        assertEquals(1, response.getPageMeta().getTotalElements());
    }

    @Test
    void getOrders_shouldSkipSummaryQuery_whenPageIsEmpty() {
        when(orderRepository.findPageForManagement(any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        PagedResponse<ManagedOrderResponse> response =
                orderManagementService.getOrders(new OrderManagementListQuery());

        assertTrue(response.getItems().isEmpty());
        verifyNoInteractions(orderItemRepository);
    }

    @Test
    void getOrder_shouldThrow_whenOrderNotFound() {
        when(orderRepository.findDetailsById(99L)).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> orderManagementService.getOrder(99L)
        );
    }

    @Test
    void getOrder_shouldReturnDetails_whenFound() {
        User user = user(7L, "buyer@example.com");
        Order order = order(10L, OrderStatus.PROCESSING, user);
        order.addItem(orderItem(product(1L, 3), 2));

        when(orderRepository.findDetailsById(10L)).thenReturn(Optional.of(order));

        ManagedOrderDetailsResponse response = orderManagementService.getOrder(10L);

        assertEquals(OrderStatus.PROCESSING, response.getStatus());
        assertEquals("buyer@example.com", response.getUserEmail());
        assertEquals(2, response.getTotalQuantity());
        assertEquals(1, response.getItems().size());
    }

    @Test
    void updateStatus_shouldAdvanceStatus_withoutTouchingStock() {
        User user = user(7L, "buyer@example.com");
        Order order = order(10L, OrderStatus.CREATED, user);

        when(orderRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(order));
        when(orderRepository.findDetailsById(10L)).thenReturn(Optional.of(order));

        ManagedOrderDetailsResponse response =
                orderManagementService.updateStatus(10L, request(OrderStatus.PAID));

        assertEquals(OrderStatus.PAID, order.getStatus());
        assertEquals(OrderStatus.PAID, response.getStatus());
        verifyNoInteractions(productRepository);
        verify(orderItemRepository, never()).findByOrderIdWithProduct(anyLong());
    }

    @Test
    void updateStatus_shouldRestoreStock_whenCancelling() {
        User user = user(7L, "buyer@example.com");
        Order order = order(10L, OrderStatus.PAID, user);
        Product product = product(1L, 3);
        OrderItem item = orderItem(product, 2);

        when(orderRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(order));
        when(orderItemRepository.findByOrderIdWithProduct(10L)).thenReturn(List.of(item));
        when(productRepository.findForUpdateByIds(List.of(1L))).thenReturn(List.of(product));
        when(orderRepository.findDetailsById(10L)).thenReturn(Optional.of(order));

        orderManagementService.updateStatus(10L, request(OrderStatus.CANCELLED));

        assertEquals(OrderStatus.CANCELLED, order.getStatus());
        assertEquals(5, product.getStock());
    }

    @Test
    void updateStatus_shouldThrow_onIllegalTransition() {
        User user = user(7L, "buyer@example.com");
        Order order = order(10L, OrderStatus.SHIPPED, user);

        when(orderRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(order));

        assertThrows(
                InvalidOperationException.class,
                () -> orderManagementService.updateStatus(10L, request(OrderStatus.CREATED))
        );

        assertEquals(OrderStatus.SHIPPED, order.getStatus());
        verifyNoInteractions(productRepository);
        verify(orderRepository, never()).findDetailsById(anyLong());
    }

    @Test
    void updateStatus_shouldThrow_onTerminalStatus() {
        User user = user(7L, "buyer@example.com");
        Order order = order(10L, OrderStatus.DELIVERED, user);

        when(orderRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(order));

        assertThrows(
                InvalidOperationException.class,
                () -> orderManagementService.updateStatus(10L, request(OrderStatus.PAID))
        );
    }

    @Test
    void updateStatus_shouldThrow_whenOrderNotFound() {
        when(orderRepository.findByIdForUpdate(anyLong())).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> orderManagementService.updateStatus(5L, request(OrderStatus.PAID))
        );
    }
}
