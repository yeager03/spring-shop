package com.yeager.shop.catalog.repository;

import com.yeager.shop.catalog.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;


@Repository
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {
    boolean existsBySlug(String slug);

    @Query("""
            SELECT p
            FROM Product p
            WHERE p.slug = :slug
              AND p.isActive = true
            """)
    Optional<Product> findActiveBySlug(
            @Param("slug") String slug
    );

    @Query("""
            SELECT COUNT(p) > 0
            FROM Product p
            WHERE p.slug = :slug
              AND p.productId <> :productId
            """)
    boolean existsSlugConflict(
            @Param("slug") String slug,
            @Param("productId") Long productId
    );
}
