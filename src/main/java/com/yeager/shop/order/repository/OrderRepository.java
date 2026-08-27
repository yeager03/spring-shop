package com.yeager.shop.order.repository;

import com.yeager.shop.order.entity.Order;
import com.yeager.shop.order.entity.OrderStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    @Query("""
            SELECT o
            FROM Order o
            WHERE o.user.userId = :userId
            """)
    Page<Order> findPageByUserId(
            @Param("userId") Long userId,
            Pageable pageable
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

    @Query("""
            SELECT DISTINCT o
            FROM Order o
                     LEFT JOIN FETCH o.items i
                     LEFT JOIN FETCH i.product
            WHERE o.orderId = :orderId
              AND o.user.userId = :userId
            """)
    Optional<Order> findDetailsByIdAndUserId(
            @Param("orderId") Long orderId,
            @Param("userId") Long userId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT o
            FROM Order o
            WHERE o.orderId = :orderId
              AND o.user.userId = :userId
            """)
    Optional<Order> findByIdAndUserIdForUpdate(
            @Param("orderId") Long orderId,
            @Param("userId") Long userId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT o
            FROM Order o
            WHERE o.orderId = :orderId
            """)
    Optional<Order> findByIdForUpdate(
            @Param("orderId") Long orderId
    );

    @Query(
            value = """
                    SELECT o
                    FROM Order o
                             JOIN FETCH o.user
                    WHERE (:status IS NULL OR o.status = :status)
                      AND (:userId IS NULL OR o.user.userId = :userId)
                    """,
            countQuery = """
                    SELECT COUNT(o)
                    FROM Order o
                    WHERE (:status IS NULL OR o.status = :status)
                      AND (:userId IS NULL OR o.user.userId = :userId)
                    """
    )
    Page<Order> findPageForManagement(
            @Param("status") OrderStatus status,
            @Param("userId") Long userId,
            Pageable pageable
    );

    @Query("""
            SELECT DISTINCT o
            FROM Order o
                     JOIN FETCH o.user
                     LEFT JOIN FETCH o.items i
                     LEFT JOIN FETCH i.product
            WHERE o.orderId = :orderId
            """)
    Optional<Order> findDetailsById(
            @Param("orderId") Long orderId
    );
}
