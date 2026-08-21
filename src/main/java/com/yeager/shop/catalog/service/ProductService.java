package com.yeager.shop.catalog.service;

import com.yeager.shop.catalog.dto.*;
import com.yeager.shop.catalog.entity.Category;
import com.yeager.shop.catalog.entity.Product;
import com.yeager.shop.catalog.entity.ProductCategory;
import com.yeager.shop.catalog.entity.ProductCategoryId;
import com.yeager.shop.catalog.repository.CategoryRepository;
import com.yeager.shop.catalog.repository.ProductCategoryRepository;
import com.yeager.shop.catalog.repository.ProductImageRepository;
import com.yeager.shop.catalog.repository.ProductRepository;
import com.yeager.shop.catalog.repository.projection.ProductMainImageProjection;
import com.yeager.shop.catalog.repository.specification.ProductSpecifications;
import com.yeager.shop.common.dto.PageMeta;
import com.yeager.shop.common.dto.PagedResponse;
import com.yeager.shop.common.exception.InvalidOperationException;
import com.yeager.shop.common.exception.ResourceAlreadyExistsException;
import com.yeager.shop.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final CategoryRepository categoryRepository;
    private final ProductCategoryRepository productCategoryRepository;

    @Transactional(readOnly = true)
    public PagedResponse<ProductListItemResponse> getProducts(ProductListQuery query) {
        Pageable pageable = createPageable(query);

        Specification<Product> specification =
                Specification
                        .where(ProductSpecifications.isActive())
                        .and(ProductSpecifications.titleContains(
                                query.getSearch()
                        ))
                        .and(ProductSpecifications.hasAnyCategory(
                                query.getCategoryIds()
                        ));

        Page<Product> page = productRepository.findAll(specification, pageable);

        List<Long> productIds = page
                .getContent()
                .stream()
                .map(Product::getProductId)
                .toList();

        List<ProductMainImageProjection> mainImages =
                productIds.isEmpty()
                        ? List.of()
                        : productImageRepository.findMainImages(productIds);

        Map<Long, String> mainImageByProductId = mainImages
                .stream()
                .collect(Collectors.toMap(
                        ProductMainImageProjection::getProductId,
                        ProductMainImageProjection::getImageKey
                ));

        List<ProductListItemResponse> items = page
                .getContent()
                .stream()
                .map(product -> {
                    String mainImageKey = mainImageByProductId.get(product.getProductId());

                    return toListItemResponse(product, mainImageKey);
                })
                .toList();

        PageMeta pageMeta = new PageMeta(
                query.getPage(),
                query.getLimit(),
                page.getTotalPages(),
                page.getTotalElements(),
                page.hasNext()
        );

        Map<String, Object> appliedQuery = createAppliedQuery(query);

        return new PagedResponse<>(items, pageMeta, appliedQuery);
    }

    @Transactional(readOnly = true)
    public ProductDetailsResponse getProduct(String slug) {
        Product product = productRepository.findActiveBySlug(slug)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Product not found by slug: " + slug)
                );

        List<ProductImageResponse> images = productImageRepository
                .findAllByProductId(product.getProductId())
                .stream()
                .map(image -> new ProductImageResponse(
                        image.getImageKey(),
                        image.getPosition()
                ))
                .toList();

        List<CategoryResponse> categories = categoryRepository
                .findActiveByProductId(product.getProductId())
                .stream()
                .map(category -> new CategoryResponse(
                        category.getCategoryId(),
                        category.getName(),
                        category.getSlug()
                ))
                .toList();

        return new ProductDetailsResponse(
                product.getProductId(),
                product.getTitle(),
                product.getSlug(),
                product.getDescription(),
                product.getPrice(),
                product.getStock() > 0,
                images,
                categories
        );
    }

    @Transactional
    public ProductManagementResponse createProduct(CreateProductRequest request) {
        String slug = normalizeSlug(request.getSlug());

        if (productRepository.existsBySlug(slug)) {
            throw new ResourceAlreadyExistsException("Product with this slug already exists");
        }

        Product product = new Product();

        product.setTitle(request.getTitle().trim());
        product.setSlug(slug);
        product.setDescription(normalizeDescription(request.getDescription()));
        product.setPrice(request.getPrice());
        product.setStock(request.getStock());

        Product savedProduct = productRepository.save(product);

        return toManagementResponse(savedProduct);
    }

    @Transactional
    public ProductManagementResponse updateProduct(Long productId, UpdateProductRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Product not found by id: " + productId)
                );

        updateTitle(product, request);
        updateSlug(product, request);
        updateDescription(product, request);
        updatePrice(product, request);
        updateStock(product, request);
        updateActive(product, request);

        return toManagementResponse(product);
    }

    @Transactional
    public void addCategory(Long productId, Long categoryId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Product not found by id: " + productId)
                );

        Category category = categoryRepository.findById(categoryId)
                .filter(Category::isActive)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Active category not found by id: " + categoryId)
                );

        ProductCategoryId id = new ProductCategoryId(productId, categoryId);

        if (productCategoryRepository.existsById(id)) {
            return;
        }

        ProductCategory productCategory = new ProductCategory();

        productCategory.setId(id);
        productCategory.setProduct(product);
        productCategory.setCategory(category);

        productCategoryRepository.save(productCategory);
    }

    @Transactional
    public void removeCategory(Long productId, Long categoryId) {
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("Product not found by id: " + productId);
        }

        if (!categoryRepository.existsById(categoryId)) {
            throw new ResourceNotFoundException("Category not found by id: " + categoryId);
        }

        productCategoryRepository.deleteLink(productId, categoryId);
    }

    private void updateTitle(Product product, UpdateProductRequest request) {
        if (request.getTitle() == null) {
            return;
        }

        String title = request.getTitle().trim();

        if (title.isEmpty()) {
            throw new InvalidOperationException("Product title must not be blank");
        }

        product.setTitle(title);
    }

    private void updateSlug(Product product, UpdateProductRequest request) {
        if (request.getSlug() == null) {
            return;
        }

        String slug = normalizeSlug(request.getSlug());

        boolean alreadyExists = productRepository.existsSlugConflict(
                slug,
                product.getProductId()
        );

        if (alreadyExists) {
            throw new ResourceAlreadyExistsException("Product with this slug already exists");
        }

        product.setSlug(slug);
    }

    private void updateDescription(Product product, UpdateProductRequest request) {
        if (!request.isDescriptionProvided()) {
            return;
        }

        product.setDescription(
                normalizeDescription(request.getDescription())
        );
    }

    private void updatePrice(Product product, UpdateProductRequest request) {
        if (request.getPrice() != null) {
            product.setPrice(request.getPrice());
        }
    }

    private void updateStock(Product product, UpdateProductRequest request) {
        if (request.getStock() != null) {
            product.setStock(request.getStock());
        }
    }

    private void updateActive(Product product, UpdateProductRequest request) {
        if (request.getActive() != null) {
            product.setActive(request.getActive());
        }
    }

    private Pageable createPageable(ProductListQuery query) {
        Sort.Direction direction = query.getOrder() == SortDirection.ASC ? Sort.Direction.ASC : Sort.Direction.DESC;

        Sort sort = Sort.by(direction, query.getSort().getProperty());

        return PageRequest.of(query.getPage() - 1, query.getLimit(), sort);
    }

    private ProductListItemResponse toListItemResponse(Product product, String imageKey) {
        return new ProductListItemResponse(
                product.getProductId(),
                product.getTitle(),
                product.getSlug(),
                product.getPrice(),
                product.getStock() > 0,
                imageKey
        );
    }

    private Map<String, Object> createAppliedQuery(ProductListQuery query) {
        Map<String, Object> filters = new LinkedHashMap<>();

        if (query.getSearch() != null && !query.getSearch().isBlank()) {
            filters.put("search", query.getSearch().trim());
        }

        if (!query.getCategoryIds().isEmpty()) {
            filters.put("categoryIds", query.getCategoryIds());
        }

        filters.put("sort", query.getSort().name().toLowerCase(Locale.ROOT));
        filters.put("order", query.getOrder().name().toLowerCase(Locale.ROOT));

        return filters;
    }

    private String normalizeSlug(String slug) {
        return slug
                .trim()
                .toLowerCase(Locale.ROOT);
    }

    private String normalizeDescription(String description) {
        if (description == null || description.isBlank()) {
            return null;
        }

        return description.trim();
    }

    private ProductManagementResponse toManagementResponse(Product product) {
        return new ProductManagementResponse(
                product.getProductId(),
                product.getTitle(),
                product.getSlug(),
                product.getDescription(),
                product.getPrice(),
                product.getStock(),
                product.isActive()
        );
    }
}
