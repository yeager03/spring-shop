package com.yeager.shop.catalog.repository;

import com.yeager.shop.catalog.entity.Category;
import com.yeager.shop.catalog.repository.projection.CategoryTreeProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    boolean existsBySlug(String slug);

    @Query("""
            SELECT pc.category
            FROM ProductCategory pc
            WHERE pc.product.productId = :productId
              AND pc.category.isActive = true
            ORDER BY pc.category.position ASC,
                     pc.category.categoryId ASC
            """)
    List<Category> findActiveByProductId(
            @Param("productId") Long productId
    );

    @Query("""
            SELECT COUNT(c) > 0
            FROM Category c
            WHERE c.slug = :slug
              AND c.categoryId <> :categoryId
            """)
    boolean existsSlugConflict(
            @Param("slug") String slug,
            @Param("categoryId") Long categoryId
    );

    @Query("""
            SELECT c.categoryId AS categoryId,
                   p.categoryId AS parentId,
                   c.name       AS name,
                   c.slug       AS slug,
                   c.position   AS position
            FROM Category c
                     LEFT JOIN c.parent p
            WHERE c.isActive = true
            ORDER BY c.position ASC,
                     c.categoryId ASC
            """)
    List<CategoryTreeProjection> findActiveTreeItems();

    @Query("""
            SELECT COUNT(c) > 0
            FROM Category c
            WHERE c.parent.categoryId = :parentId
              AND c.isActive = true
            """)
    boolean hasActiveChildren(
            @Param("parentId") Long parentId
    );

    @Query("""
            SELECT c
            FROM Category c
            WHERE c.slug = :slug
              AND c.isActive = true
            """)
    Optional<Category> findActiveBySlug(
            @Param("slug") String slug
    );
}
