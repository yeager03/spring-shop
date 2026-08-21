package com.yeager.shop.catalog.repository;

import com.yeager.shop.catalog.entity.ProductCategory;
import com.yeager.shop.catalog.entity.ProductCategoryId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductCategoryRepository extends JpaRepository<ProductCategory, ProductCategoryId> {
    @Modifying
    @Query("""
            DELETE
            FROM ProductCategory pc
            WHERE pc.id.productId = :productId
              AND pc.id.categoryId = :categoryId
            """)
    int deleteLink(
            @Param("productId") Long productId,
            @Param("categoryId") Long categoryId
    );
}
