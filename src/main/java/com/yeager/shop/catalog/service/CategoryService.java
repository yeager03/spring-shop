package com.yeager.shop.catalog.service;

import com.yeager.shop.catalog.dto.*;
import com.yeager.shop.catalog.entity.Category;
import com.yeager.shop.catalog.repository.CategoryRepository;
import com.yeager.shop.catalog.repository.projection.CategoryTreeProjection;
import com.yeager.shop.common.exception.InvalidOperationException;
import com.yeager.shop.common.exception.ResourceAlreadyExistsException;
import com.yeager.shop.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

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

    @Transactional(readOnly = true)
    public List<CategoryTreeResponse> getCategories() {
        List<CategoryTreeProjection> categories = categoryRepository.findActiveTreeItems();

        Map<Long, CategoryTreeResponse> nodes = new LinkedHashMap<>();

        for (CategoryTreeProjection category : categories) {
            nodes.put(
                    category.getCategoryId(),
                    new CategoryTreeResponse(
                            category.getCategoryId(),
                            category.getName(),
                            category.getSlug()
                    )
            );
        }

        List<CategoryTreeResponse> roots = new ArrayList<>();

        for (CategoryTreeProjection category : categories) {
            CategoryTreeResponse node = nodes.get(category.getCategoryId());

            Long parentId = category.getParentId();

            if (parentId == null) {
                roots.add(node);

                continue;
            }

            CategoryTreeResponse parent = nodes.get(parentId);

            if (parent != null) {
                parent.addChild(node);
            }
        }

        return roots;
    }

    @Transactional(readOnly = true)
    public CategoryDetailsResponse getCategory(String slug) {
        Category category = categoryRepository.findActiveBySlug(slug)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Category not found by slug: " + slug)
                );

        CategoryResponse parent = null;

        if (category.getParent() != null) {
            Category parentCategory = category.getParent();

            parent = new CategoryResponse(
                    parentCategory.getCategoryId(),
                    parentCategory.getName(),
                    parentCategory.getSlug()
            );
        }

        return new CategoryDetailsResponse(
                category.getCategoryId(),
                category.getName(),
                category.getSlug(),
                category.getPosition(),
                parent
        );
    }


    @Transactional
    public CategoryResponse updateCategory(Long categoryId, UpdateCategoryRequest request) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Category not found by id: " + categoryId)
                );

        updateName(category, request);
        updateSlug(category, request);
        updatePosition(category, request);
        updateParent(category, request);
        updateActive(category, request);

        return new CategoryResponse(
                category.getCategoryId(),
                category.getName(),
                category.getSlug()
        );
    }

    @Transactional
    public void deactivateCategory(Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Category not found by id: " + categoryId)
                );

        if (!category.isActive()) {
            return;
        }

        validateDeactivation(category);

        category.setActive(false);
    }

    private void updateName(Category category, UpdateCategoryRequest request) {
        if (request.getName() == null) {
            return;
        }

        String name = request.getName().trim();

        if (name.isEmpty()) {
            throw new InvalidOperationException("Category name must not be blank");
        }

        category.setName(name);
    }

    private void updateSlug(Category category, UpdateCategoryRequest request) {
        if (request.getSlug() == null) {
            return;
        }

        String slug = normalizeSlug(request.getSlug());

        boolean alreadyExists = categoryRepository.existsSlugConflict(slug, category.getCategoryId());

        if (alreadyExists) {
            throw new ResourceAlreadyExistsException("Category with this slug already exists");
        }

        category.setSlug(slug);
    }

    private void updatePosition(Category category, UpdateCategoryRequest request) {
        if (request.getPosition() != null) {
            category.setPosition(request.getPosition());
        }
    }

    private void updateParent(Category category, UpdateCategoryRequest request) {
        if (!request.isParentIdProvided()) {
            return;
        }

        Long parentId = request.getParentId();

        if (parentId == null) {
            category.setParent(null);

            return;
        }

        Category parent = categoryRepository.findById(parentId)
                .filter(Category::isActive)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Active parent category not found by id: " + parentId)
                );

        validateParent(category, parent);

        category.setParent(parent);
    }

    private void validateParent(Category category, Category parent) {
        Category current = parent;

        while (current != null) {

            if (category.getCategoryId().equals(
                    current.getCategoryId()
            )) {
                throw new InvalidOperationException("Category hierarchy cannot contain cycles");
            }

            current = current.getParent();
        }
    }

    private void updateActive(Category category, UpdateCategoryRequest request) {
        if (request.getActive() == null) {
            return;
        }

        boolean active = request.getActive();

        if (!active) {
            validateDeactivation(category);
        } else {
            validateActivation(category);
        }

        category.setActive(active);
    }

    private void validateDeactivation(Category category) {
        if (categoryRepository.hasActiveChildren(category.getCategoryId())) {
            throw new InvalidOperationException(
                    "Category with active child categories cannot be deactivated"
            );
        }
    }

    private void validateActivation(Category category) {
        Category parent = category.getParent();

        if (parent != null && !parent.isActive()) {
            throw new InvalidOperationException(
                    "Category cannot be activated while its parent is inactive"
            );
        }
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
