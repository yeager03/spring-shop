package com.yeager.shop.catalog.repository.specification;

import com.yeager.shop.catalog.entity.Product;
import com.yeager.shop.catalog.entity.ProductCategory;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Locale;

public final class ProductSpecifications {
    private ProductSpecifications() {
    }

    public static Specification<Product> isActive() {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.isTrue(root.get("isActive"));
    }

    public static Specification<Product> titleContains(String search) {
        return (root, query, criteriaBuilder) -> {

            if (search == null || search.isBlank()) {
                return criteriaBuilder.conjunction();
            }

            String pattern = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";

            return criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("title")),
                    pattern
            );
        };
    }

    public static Specification<Product> hasAnyCategory(List<Long> categoryIds) {
        return (root, query, criteriaBuilder) -> {

            if (categoryIds == null || categoryIds.isEmpty()) {
                return criteriaBuilder.conjunction();
            }

            Subquery<Integer> subquery = query.subquery(Integer.class);

            Root<ProductCategory> productCategory = subquery.from(ProductCategory.class);

            subquery.select(criteriaBuilder.literal(1));

            subquery.where(
                    criteriaBuilder.equal(
                            productCategory.get("product"),
                            root
                    ),
                    productCategory
                            .get("category")
                            .get("categoryId")
                            .in(categoryIds)
            );

            return criteriaBuilder.exists(subquery);
        };
    }
}


