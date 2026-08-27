package com.yeager.shop.order.controller;

import com.yeager.shop.common.dto.PageMeta;
import com.yeager.shop.common.dto.PagedResponse;
import com.yeager.shop.common.exception.InvalidOperationException;
import com.yeager.shop.common.exception.ResourceNotFoundException;
import com.yeager.shop.order.dto.ManagedOrderDetailsResponse;
import com.yeager.shop.order.dto.ManagedOrderResponse;
import com.yeager.shop.order.entity.OrderStatus;
import com.yeager.shop.order.service.OrderManagementService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderManagementController.class)
@AutoConfigureMockMvc(addFilters = false)
class OrderManagementControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderManagementService orderManagementService;

    private ManagedOrderDetailsResponse detailsResponse(OrderStatus status) {
        return new ManagedOrderDetailsResponse(
                10L,
                7L,
                "buyer@example.com",
                status,
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
    void getOrders_shouldReturn200AndPagedResponse() throws Exception {
        PagedResponse<ManagedOrderResponse> response = new PagedResponse<>(
                List.of(new ManagedOrderResponse(
                        10L,
                        7L,
                        "buyer@example.com",
                        OrderStatus.CREATED,
                        new BigDecimal("20.00"),
                        2,
                        1,
                        Instant.parse("2026-01-01T00:00:00Z"),
                        Instant.parse("2026-01-01T00:00:00Z")
                )),
                new PageMeta(1, 20, 1, 1, false),
                Map.of("page", 1, "limit", 20)
        );

        when(orderManagementService.getOrders(any())).thenReturn(response);

        mockMvc.perform(get("/management/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items[0].orderId").value(10))
                .andExpect(jsonPath("$.items[0].userEmail").value("buyer@example.com"))
                .andExpect(jsonPath("$.pageMeta.totalElements").value(1));
    }

    @Test
    void getOrders_shouldReturn400_whenStatusIsUnknown() throws Exception {
        mockMvc.perform(get("/management/orders").param("status", "BOGUS"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors[0].field").value("status"));

        verifyNoInteractions(orderManagementService);
    }

    @Test
    void getOrders_shouldReturn400_whenLimitExceedsMax() throws Exception {
        mockMvc.perform(get("/management/orders").param("limit", "50"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.errors[0].field").value("limit"));

        verifyNoInteractions(orderManagementService);
    }

    @Test
    void getOrder_shouldReturn404_whenOrderDoesNotExist() throws Exception {
        when(orderManagementService.getOrder(99L))
                .thenThrow(new ResourceNotFoundException("Order not found by id: 99"));

        mockMvc.perform(get("/management/orders/{orderId}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource not found"))
                .andExpect(jsonPath("$.detail").value("Order not found by id: 99"));
    }

    @Test
    void getOrder_shouldReturn200AndDetails() throws Exception {
        when(orderManagementService.getOrder(10L)).thenReturn(detailsResponse(OrderStatus.PROCESSING));

        mockMvc.perform(get("/management/orders/{orderId}", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(10))
                .andExpect(jsonPath("$.userId").value(7))
                .andExpect(jsonPath("$.status").value("PROCESSING"));
    }

    @Test
    void updateStatus_shouldReturn200AndUpdatedOrder() throws Exception {
        when(orderManagementService.updateStatus(eq(10L), any()))
                .thenReturn(detailsResponse(OrderStatus.PAID));

        mockMvc.perform(
                        patch("/management/orders/{orderId}/status", 10L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"status\":\"PAID\"}")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(10))
                .andExpect(jsonPath("$.status").value("PAID"));
    }

    @Test
    void updateStatus_shouldReturn400WithAllowedValues_whenStatusIsUnknown() throws Exception {
        mockMvc.perform(
                        patch("/management/orders/{orderId}/status", 10L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"status\":\"abcd\"}")
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.errors[0].field").value("status"))
                .andExpect(jsonPath("$.errors[0].message").value(
                        "Status must be one of: CREATED, PAID, PROCESSING, SHIPPED, DELIVERED, CANCELLED"
                ));

        verifyNoInteractions(orderManagementService);
    }

    @Test
    void updateStatus_shouldReturn400_whenStatusMissing() throws Exception {
        mockMvc.perform(
                        patch("/management/orders/{orderId}/status", 10L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}")
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors[0].field").value("status"));

        verifyNoInteractions(orderManagementService);
    }

    @Test
    void updateStatus_shouldReturn400_whenTransitionIllegal() throws Exception {
        when(orderManagementService.updateStatus(eq(10L), any()))
                .thenThrow(new InvalidOperationException(
                        "Cannot change order status from SHIPPED to CREATED"
                ));

        mockMvc.perform(
                        patch("/management/orders/{orderId}/status", 10L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"status\":\"CREATED\"}")
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid operation"))
                .andExpect(jsonPath("$.detail")
                        .value("Cannot change order status from SHIPPED to CREATED"));
    }
}
