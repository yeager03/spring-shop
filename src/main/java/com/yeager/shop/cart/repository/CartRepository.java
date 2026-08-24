package com.yeager.shop.cart.repository;

import com.yeager.shop.cart.entity.Cart;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {
    @Query("""
            SELECT c
            FROM Cart c
            WHERE c.user.userId = :userId
            """)
    Optional<Cart> findByUserId(
            @Param("userId") Long userId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT c
            FROM Cart c
            WHERE c.user.userId = :userId
            """)
    Optional<Cart> findForUpdateByUserId(
            @Param("userId") Long userId
    );
}
