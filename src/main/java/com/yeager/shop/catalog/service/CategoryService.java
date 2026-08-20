package com.yeager.shop.catalog.service;

import com.yeager.shop.catalog.dto.CategoryResponse;
import com.yeager.shop.catalog.dto.CreateCategoryRequest;
import com.yeager.shop.catalog.entity.Category;
import com.yeager.shop.catalog.repository.CategoryRepository;
import com.yeager.shop.common.exception.ResourceAlreadyExistsException;
import com.yeager.shop.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class CategoryService {
    private final CategoryRepository categoryRepository;

    @Transactional
    public CategoryResponse createCategory(CreateCategoryRequest request) {
        String slug = normalizeSlug(request.getSlug());

        if (categoryRepository.existsBySlug(slug)) {
            throw new ResourceAlreadyExistsException("Category with this slug already exists");
        }

        Category parent = findParent(request.getParentId());

        Category category = new Category();

        category.setName(request.getName().trim());
        category.setSlug(slug);
        category.setParent(parent);
        category.setPosition(request.getPosition());

        Category savedCategory =
                categoryRepository.save(category);

        return new CategoryResponse(
                savedCategory.getCategoryId(),
                savedCategory.getName(),
                savedCategory.getSlug()
        );
    }

    private Category findParent(Long parentId) {
        if (parentId == null) {
            return null;
        }

        return categoryRepository.findById(parentId)
                .filter(Category::isActive)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Active parent category not found by id: " + parentId)
                );
    }

    private String normalizeSlug(String slug) {
        return slug
                .trim()
                .toLowerCase(Locale.ROOT);
    }
}
