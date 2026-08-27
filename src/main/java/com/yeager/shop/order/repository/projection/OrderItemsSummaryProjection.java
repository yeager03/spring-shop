package com.yeager.shop.order.repository.projection;

public interface OrderItemsSummaryProjection {
    Long getOrderId();

    Long getTotalQuantity();

    Long getItemCount();
}
