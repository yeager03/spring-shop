package com.yeager.shop.cart.repository;

import com.yeager.shop.cart.entity.CartItem;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepository extends CrudRepository<CartItem, Long> {
    @Query("""
            SELECT ci
            FROM CartItem ci
                     JOIN FETCH ci.product
            WHERE ci.cart.cartId = :cartId
            ORDER BY ci.createdAt ASC
            """)
    List<CartItem> findAllByCartId(
            @Param("cartId") Long cartId
    );

    @Query("""
            SELECT ci
            FROM CartItem ci
            WHERE ci.cart.cartId = :cartId
              AND ci.product.productId = :productId
            """)
    Optional<CartItem> findByCartIdAndProductId(
            @Param("cartId") Long cartId,
            @Param("productId") Long productId
    );

    @Modifying
    @Query("""
            DELETE
            FROM CartItem ci
            WHERE ci.cart.cartId = :cartId
              AND ci.product.productId = :productId
            """)
    int deleteItem(
            @Param("cartId") Long cartId,
            @Param("productId") Long productId
    );

    @Modifying
    @Query("""
            DELETE
            FROM CartItem ci
            WHERE ci.cart.cartId = :cartId
            """)
    int deleteAllItems(
            @Param("cartId") Long cartId
    );
}
