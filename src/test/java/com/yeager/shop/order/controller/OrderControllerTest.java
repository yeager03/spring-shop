package com.yeager.shop.order.controller;

import com.yeager.shop.authentication.security.AuthenticatedUserPrincipal;
import com.yeager.shop.common.dto.PageMeta;
import com.yeager.shop.common.dto.PagedResponse;
import com.yeager.shop.common.exception.InvalidOperationException;
import com.yeager.shop.common.exception.ResourceNotFoundException;
import com.yeager.shop.order.dto.CreateOrderRequest;
import com.yeager.shop.order.dto.OrderDetailsResponse;
import com.yeager.shop.order.dto.OrderResponse;
import com.yeager.shop.order.entity.OrderStatus;
import com.yeager.shop.order.service.OrderService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
@AutoConfigureMockMvc(addFilters = false)
class OrderControllerTest {
    private static final Long USER_ID = 42L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderService orderService;

    @org.junit.jupiter.api.BeforeEach
    void authenticate() {
        AuthenticatedUserPrincipal principal = new AuthenticatedUserPrincipal(
                USER_ID,
                com.yeager.shop.user.entity.UserRole.CUSTOMER,
                Map.of(),
                List.of()
        );

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, "n/a", List.of())
        );
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private String requestBody() {
        return """
                {
                  "recipientName": "John Doe",
                  "recipientPhone": "+1000000",
                  "country": "Wonderland",
                  "city": "Capital",
                  "street": "Main",
                  "house": "1"
                }
                """;
    }

    private OrderDetailsResponse detailsResponse() {
        return new OrderDetailsResponse(
                10L,
                OrderStatus.CREATED,
                new BigDecimal("20.00"),
                2,
                "John Doe",
                "+1000000",
                "Wonderland",
                "Capital",
                "Main",
                "1",
                null,
                null,
                List.of(),
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-01-01T00:00:00Z")
        );
    }

    @Test
    void checkout_shouldReturn201AndOrder() throws Exception {
        when(orderService.checkout(eq(USER_ID), any(CreateOrderRequest.class)))
                .thenReturn(detailsResponse());

        mockMvc.perform(
                        post("/orders")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody())
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").value(10))
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.totalAmount").value(20.00))
                .andExpect(jsonPath("$.totalQuantity").value(2));

        verify(orderService).checkout(eq(USER_ID), any(CreateOrderRequest.class));
    }

    @Test
    void checkout_shouldReturn400_whenCartIsEmpty() throws Exception {
        when(orderService.checkout(eq(USER_ID), any(CreateOrderRequest.class)))
                .thenThrow(new InvalidOperationException("Cart is empty"));

        mockMvc.perform(
                        post("/orders")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody())
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid operation"))
                .andExpect(jsonPath("$.detail").value("Cart is empty"));
    }

    @Test
    void checkout_shouldReturn400_whenRecipientNameBlank() throws Exception {
        String body = """
                {
                  "recipientName": "  ",
                  "recipientPhone": "+1000000",
                  "country": "Wonderland",
                  "city": "Capital",
                  "street": "Main",
                  "house": "1"
                }
                """;

        mockMvc.perform(
                        post("/orders")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors[0].field").value("recipientName"));

        verifyNoInteractions(orderService);
    }

    @Test
    void getOrder_shouldReturn404_whenOrderDoesNotExist() throws Exception {
        when(orderService.getOrder(USER_ID, 99L))
                .thenThrow(new ResourceNotFoundException("Order not found by id: 99"));

        mockMvc.perform(get("/orders/{orderId}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource not found"))
                .andExpect(jsonPath("$.detail").value("Order not found by id: 99"));
    }

    @Test
    void getOrders_shouldReturn200AndPagedResponse() throws Exception {
        PagedResponse<OrderResponse> response = new PagedResponse<>(
                List.of(new OrderResponse(
                        10L,
                        OrderStatus.CREATED,
                        new BigDecimal("20.00"),
                        2,
                        1,
                        Instant.parse("2026-01-01T00:00:00Z")
                )),
                new PageMeta(1, 20, 1, 1, false),
                Map.of("page", 1, "limit", 20)
        );

        when(orderService.getOrders(eq(USER_ID), any())).thenReturn(response);

        mockMvc.perform(get("/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items[0].orderId").value(10))
                .andExpect(jsonPath("$.pageMeta.page").value(1))
                .andExpect(jsonPath("$.pageMeta.totalElements").value(1));
    }

    @Test
    void getOrders_shouldReturn400_whenLimitExceedsMax() throws Exception {
        mockMvc.perform(get("/orders").param("limit", "50"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.errors[0].field").value("limit"));

        verifyNoInteractions(orderService);
    }

    @Test
    void cancelOrder_shouldReturn204() throws Exception {
        mockMvc.perform(post("/orders/{orderId}/cancel", 5L))
                .andExpect(status().isNoContent());

        verify(orderService).cancelOrder(USER_ID, 5L);
    }
}
