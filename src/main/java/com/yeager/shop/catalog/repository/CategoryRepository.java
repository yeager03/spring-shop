package com.yeager.shop.catalog.repository;

import com.yeager.shop.catalog.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    @Query("""
            SELECT pc.category
            FROM ProductCategory pc
            WHERE pc.product.productId = :productId
              AND pc.category.isActive = true
            ORDER BY pc.category.position ASC
            """)
    List<Category> findActiveByProductId(
            @Param("productId") Long productId
    );
}
