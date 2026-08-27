package com.yeager.shop.order.service;

import com.yeager.shop.cart.entity.Cart;
import com.yeager.shop.cart.entity.CartItem;
import com.yeager.shop.cart.repository.CartItemRepository;
import com.yeager.shop.cart.repository.CartRepository;
import com.yeager.shop.catalog.entity.Product;
import com.yeager.shop.catalog.repository.ProductRepository;
import com.yeager.shop.common.exception.InvalidOperationException;
import com.yeager.shop.common.exception.ResourceNotFoundException;
import com.yeager.shop.common.dto.PagedResponse;
import com.yeager.shop.order.dto.CreateOrderRequest;
import com.yeager.shop.order.dto.OrderDetailsResponse;
import com.yeager.shop.order.dto.OrderListQuery;
import com.yeager.shop.order.dto.OrderResponse;
import com.yeager.shop.order.entity.Order;
import com.yeager.shop.order.entity.OrderItem;
import com.yeager.shop.order.entity.OrderStatus;
import com.yeager.shop.order.repository.OrderItemRepository;
import com.yeager.shop.order.repository.OrderRepository;
import com.yeager.shop.order.repository.projection.OrderItemsSummaryProjection;
import com.yeager.shop.user.entity.User;
import com.yeager.shop.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {
    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private OrderService orderService;

    private CreateOrderRequest sampleRequest() {
        CreateOrderRequest request = new CreateOrderRequest();

        request.setRecipientName("John Doe");
        request.setRecipientPhone("+1000000");
        request.setCountry("Wonderland");
        request.setCity("Capital");
        request.setStreet("Main");
        request.setHouse("1");

        return request;
    }

    private Product product(long id, String title, String price, int stock, boolean active) {
        Product product = new Product();

        product.setProductId(id);
        product.setTitle(title);
        product.setSlug("slug-" + id);
        product.setPrice(new BigDecimal(price));
        product.setStock(stock);
        product.setActive(active);

        return product;
    }

    private CartItem cartItem(Cart cart, Product product, int quantity) {
        CartItem item = new CartItem();

        item.setCart(cart);
        item.setProduct(product);
        item.setQuantity(quantity);

        return item;
    }

    @Test
    void checkout_shouldCreateOrder_decrementStock_andClearCart() {
        Long userId = 42L;

        User user = new User();
        Cart cart = new Cart();
        cart.setCartId(7L);

        Product product = product(1L, "Pizza", "10.00", 5, true);

        when(userRepository.findForUpdateById(userId)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findAllByCartId(7L))
                .thenReturn(List.of(cartItem(cart, product, 2)));
        when(productRepository.findForUpdateByIds(any())).thenReturn(List.of(product));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderDetailsResponse response = orderService.checkout(userId, sampleRequest());

        assertEquals(OrderStatus.CREATED, response.getStatus());
        assertEquals(0, new BigDecimal("20.00").compareTo(response.getTotalAmount()));
        assertEquals(2, response.getTotalQuantity());
        assertEquals(1, response.getItems().size());
        assertEquals("Pizza", response.getItems().get(0).getProductTitle());
        assertEquals(3, product.getStock());

        verify(cartItemRepository).deleteAllItems(7L);
    }

    @Test
    void checkout_shouldThrow_whenCartMissing() {
        Long userId = 42L;

        when(userRepository.findForUpdateById(userId)).thenReturn(Optional.of(new User()));
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.empty());

        assertThrows(
                InvalidOperationException.class,
                () -> orderService.checkout(userId, sampleRequest())
        );

        verify(orderRepository, never()).save(any());
    }

    @Test
    void checkout_shouldThrow_whenCartHasNoItems() {
        Long userId = 42L;

        Cart cart = new Cart();
        cart.setCartId(7L);

        when(userRepository.findForUpdateById(userId)).thenReturn(Optional.of(new User()));
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findAllByCartId(7L)).thenReturn(List.of());

        assertThrows(
                InvalidOperationException.class,
                () -> orderService.checkout(userId, sampleRequest())
        );

        verify(orderRepository, never()).save(any());
    }

    @Test
    void checkout_shouldThrow_whenStockInsufficient() {
        Long userId = 42L;

        Cart cart = new Cart();
        cart.setCartId(7L);

        Product product = product(1L, "Pizza", "10.00", 1, true);

        when(userRepository.findForUpdateById(userId)).thenReturn(Optional.of(new User()));
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findAllByCartId(7L))
                .thenReturn(List.of(cartItem(cart, product, 2)));
        when(productRepository.findForUpdateByIds(any())).thenReturn(List.of(product));

        assertThrows(
                InvalidOperationException.class,
                () -> orderService.checkout(userId, sampleRequest())
        );

        verify(orderRepository, never()).save(any());
    }

    @Test
    void checkout_shouldThrow_whenProductInactive() {
        Long userId = 42L;

        Cart cart = new Cart();
        cart.setCartId(7L);

        Product product = product(1L, "Pizza", "10.00", 5, false);

        when(userRepository.findForUpdateById(userId)).thenReturn(Optional.of(new User()));
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findAllByCartId(7L))
                .thenReturn(List.of(cartItem(cart, product, 1)));
        when(productRepository.findForUpdateByIds(any())).thenReturn(List.of(product));

        assertThrows(
                InvalidOperationException.class,
                () -> orderService.checkout(userId, sampleRequest())
        );

        verify(orderRepository, never()).save(any());
    }

    @Test
    void getOrder_shouldThrow_whenOrderNotFound() {
        when(orderRepository.findDetailsByIdAndUserId(99L, 42L)).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> orderService.getOrder(42L, 99L)
        );
    }

    @Test
    void getOrders_shouldMapPage_fromItemSummaries_withoutTouchingItemsCollection() {
        Long userId = 42L;

        Order order = new Order();
        order.setOrderId(10L);
        order.setStatus(OrderStatus.CREATED);
        order.setTotalAmount(new BigDecimal("20.00"));
        order.setCreatedAt(Instant.parse("2026-01-01T00:00:00Z"));

        when(orderRepository.findPageByUserId(anyLong(), any()))
                .thenReturn(new PageImpl<>(List.of(order), PageRequest.of(0, 20), 1));

        OrderItemsSummaryProjection summary = mock(OrderItemsSummaryProjection.class);
        when(summary.getOrderId()).thenReturn(10L);
        when(summary.getTotalQuantity()).thenReturn(5L);
        when(summary.getItemCount()).thenReturn(2L);
        when(orderItemRepository.findItemSummariesByOrderIds(List.of(10L)))
                .thenReturn(List.of(summary));

        PagedResponse<OrderResponse> response =
                orderService.getOrders(userId, new OrderListQuery());

        assertEquals(1, response.getItems().size());

        OrderResponse row = response.getItems().get(0);

        assertEquals(10L, row.getOrderId());
        assertEquals(OrderStatus.CREATED, row.getStatus());
        assertEquals(5, row.getTotalQuantity());
        assertEquals(2, row.getItemCount());
        assertEquals(1, response.getPageMeta().getTotalElements());
    }

    @Test
    void getOrders_shouldSkipSummaryQuery_whenPageIsEmpty() {
        when(orderRepository.findPageByUserId(anyLong(), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        PagedResponse<OrderResponse> response =
                orderService.getOrders(42L, new OrderListQuery());

        assertTrue(response.getItems().isEmpty());
        verify(orderItemRepository, never()).findItemSummariesByOrderIds(any());
    }

    @Test
    void cancelOrder_shouldSetCancelled_andRestoreStock() {
        Long userId = 42L;

        Order order = new Order();
        order.setStatus(OrderStatus.CREATED);

        Product product = product(1L, "Pizza", "10.00", 3, true);

        OrderItem item = new OrderItem();
        item.setProduct(product);
        item.setQuantity(2);

        when(orderRepository.findByIdAndUserIdForUpdate(5L, userId)).thenReturn(Optional.of(order));
        when(orderItemRepository.findByOrderIdWithProduct(5L)).thenReturn(List.of(item));
        when(productRepository.findForUpdateByIds(any())).thenReturn(List.of(product));

        orderService.cancelOrder(userId, 5L);

        assertEquals(OrderStatus.CANCELLED, order.getStatus());
        assertEquals(5, product.getStock());
    }

    @Test
    void cancelOrder_shouldThrow_whenStatusNotCancellable() {
        Long userId = 42L;

        Order order = new Order();
        order.setStatus(OrderStatus.SHIPPED);

        when(orderRepository.findByIdAndUserIdForUpdate(5L, userId)).thenReturn(Optional.of(order));

        assertThrows(
                InvalidOperationException.class,
                () -> orderService.cancelOrder(userId, 5L)
        );

        verify(productRepository, never()).findForUpdateByIds(any());
    }

    @Test
    void cancelOrder_shouldThrow_whenOrderNotFound() {
        when(orderRepository.findByIdAndUserIdForUpdate(anyLong(), anyLong())).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> orderService.cancelOrder(42L, 5L)
        );
    }
}
