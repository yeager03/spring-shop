package com.yeager.shop.order.repository;

import com.yeager.shop.order.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    @Query("""
            SELECT o
            FROM Order o
            WHERE o.user.userId = :userId
            ORDER BY o.createdAt DESC
            """)
    List<Order> findByUserId(
            @Param("userId") Long userId
    );

    @Query("""
            SELECT o
            FROM Order o
            WHERE o.orderId = :orderId
              AND o.user.userId = :userId
            """)
    Optional<Order> findByIdAndUserId(
            @Param("orderId") Long orderId,
            @Param("userId") Long userId
    );
}
