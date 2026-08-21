package com.yeager.shop.catalog.repository;

import com.yeager.shop.catalog.entity.ProductImage;
import com.yeager.shop.catalog.repository.projection.ProductMainImageProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

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

    @Query("""
            SELECT COUNT(pi) > 0
            FROM ProductImage pi
            WHERE pi.product.productId = :productId
              AND pi.position = :position
            """)
    boolean existsPosition(
            @Param("productId") Long productId,
            @Param("position") int position
    );

    @Query("""
            SELECT pi
            FROM ProductImage pi
            WHERE pi.imageId = :imageId
              AND pi.product.productId = :productId
            """)
    Optional<ProductImage> findByIdAndProductId(
            @Param("imageId") Long imageId,
            @Param("productId") Long productId
    );

    @Query("""
            SELECT pi
            FROM ProductImage pi
            WHERE pi.product.productId = :productId
              AND pi.position = :position
            """)
    Optional<ProductImage> findAtPosition(
            @Param("productId") Long productId,
            @Param("position") int position
    );

    @Query("""
            SELECT COALESCE(MAX(pi.position), 0)
            FROM ProductImage pi
            WHERE pi.product.productId = :productId
            """)
    int findMaxPosition(
            @Param("productId") Long productId
    );

    @Query("""
            SELECT pi
            FROM ProductImage pi
            WHERE pi.product.productId = :productId
              AND pi.position = (SELECT MIN(pi2.position)
                                 FROM ProductImage pi2
                                 WHERE pi2.product.productId = :productId)
            """)
    Optional<ProductImage> findLowestPositionImage(
            @Param("productId") Long productId
    );
}
