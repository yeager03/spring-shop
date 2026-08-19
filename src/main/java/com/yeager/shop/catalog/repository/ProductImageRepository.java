package com.yeager.shop.catalog.repository;

import com.yeager.shop.catalog.entity.ProductImage;
import com.yeager.shop.catalog.repository.projection.ProductMainImageProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {
    @Query("""
            SELECT pi.product.productId as productId,
                   pi.imageKey          as imageKey
            FROM ProductImage pi
            WHERE pi.product.productId IN :productIds
              AND pi.position = 0
            """)
    List<ProductMainImageProjection> findMainImages(
            @Param("productIds") Collection<Long> productIds
    );

    @Query("""
            SELECT pi
            FROM ProductImage pi
            WHERE pi.product.productId = :productId
            ORDER BY pi.position ASC
            """)
    List<ProductImage> findAllByProductId(
            @Param("productId") Long productId
    );
}
