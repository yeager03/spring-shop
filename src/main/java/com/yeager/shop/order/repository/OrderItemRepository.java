package com.yeager.shop.order.repository;

import com.yeager.shop.order.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
}
