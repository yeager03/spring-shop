package com.yeager.shop.order.repository;

import com.yeager.shop.order.entity.OrderItem;
import com.yeager.shop.order.repository.projection.OrderItemsSummaryProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    @Query("""
            SELECT oi
            FROM OrderItem oi
            WHERE oi.order.orderId = :orderId
            ORDER BY oi.createdAt ASC
            """)
    List<OrderItem> findByOrderId(
            @Param("orderId") Long orderId
    );

    @Query("""
            SELECT oi
            FROM OrderItem oi
                     LEFT JOIN FETCH oi.product
            WHERE oi.order.orderId = :orderId
            ORDER BY oi.createdAt ASC
            """)
    List<OrderItem> findByOrderIdWithProduct(
            @Param("orderId") Long orderId
    );

    @Query("""
            SELECT oi.order.orderId AS orderId,
                   COALESCE(SUM(oi.quantity), 0) AS totalQuantity,
                   COUNT(oi) AS itemCount
            FROM OrderItem oi
            WHERE oi.order.orderId IN :orderIds
            GROUP BY oi.order.orderId
            """)
    List<OrderItemsSummaryProjection> findItemSummariesByOrderIds(
            @Param("orderIds") Collection<Long> orderIds
    );
}
