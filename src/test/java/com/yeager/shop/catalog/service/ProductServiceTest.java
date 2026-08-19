package com.yeager.shop.catalog.service;

import com.yeager.shop.catalog.dto.*;
import com.yeager.shop.catalog.entity.Category;
import com.yeager.shop.catalog.entity.Product;
import com.yeager.shop.catalog.entity.ProductImage;
import com.yeager.shop.catalog.repository.CategoryRepository;
import com.yeager.shop.catalog.repository.ProductImageRepository;
import com.yeager.shop.catalog.repository.ProductRepository;
import com.yeager.shop.catalog.repository.projection.ProductMainImageProjection;
import com.yeager.shop.common.dto.PagedResponse;
import com.yeager.shop.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {
    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductImageRepository productImageRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private ProductService productService;

    @Test
    void getProducts_shouldReturnEmptyPage_whenProductsDoNotExist() {
        ProductListQuery query = new ProductListQuery();

        Page<Product> emptyPage = new PageImpl<>(
                List.of(),
                PageRequest.of(0, 20),
                0
        );

        when(productRepository.findAll(
                any(Specification.class),
                any(Pageable.class)
        )).thenReturn(emptyPage);

        PagedResponse<ProductListItemResponse> result = productService.getProducts(query);

        assertTrue(result.getItems().isEmpty());

        assertEquals(1, result.getPageMeta().getPage());
        assertEquals(20, result.getPageMeta().getLimit());
        assertEquals(0, result.getPageMeta().getTotalPages());
        assertEquals(0, result.getPageMeta().getTotalElements());
        assertFalse(result.getPageMeta().isHasNext());

        assertEquals("created_at", result.getAppliedQuery().get("sort"));
        assertEquals("desc", result.getAppliedQuery().get("order"));

        verifyNoInteractions(productImageRepository);
    }

    @Test
    void getProducts_shouldReturnProducts_whenProductsExist() {
        ProductListQuery query = new ProductListQuery();

        Product product1 = mock(Product.class);
        Product product2 = mock(Product.class);

        when(product1.getProductId()).thenReturn(1L);
        when(product1.getTitle()).thenReturn("Pizza");
        when(product1.getSlug()).thenReturn("pizza");
        when(product1.getPrice()).thenReturn(new BigDecimal("2500.00"));
        when(product1.getStock()).thenReturn(10);

        when(product2.getProductId()).thenReturn(2L);
        when(product2.getTitle()).thenReturn("Burger");
        when(product2.getSlug()).thenReturn("burger");
        when(product2.getPrice()).thenReturn(new BigDecimal("1800.00"));
        when(product2.getStock()).thenReturn(0);

        Page<Product> page = new PageImpl<>(
                List.of(product1, product2),
                PageRequest.of(0, 20),
                2
        );

        when(productRepository.findAll(
                any(Specification.class),
                any(Pageable.class)
        )).thenReturn(page);

        ProductMainImageProjection image1 =
                mock(ProductMainImageProjection.class);

        ProductMainImageProjection image2 =
                mock(ProductMainImageProjection.class);

        when(image1.getProductId()).thenReturn(1L);
        when(image1.getImageKey()).thenReturn("pizza.jpg");

        when(image2.getProductId()).thenReturn(2L);
        when(image2.getImageKey()).thenReturn("burger.jpg");

        when(productImageRepository.findMainImages(List.of(1L, 2L)))
                .thenReturn(List.of(image1, image2));

        PagedResponse<ProductListItemResponse> result =
                productService.getProducts(query);

        assertEquals(2, result.getItems().size());

        assertEquals(1L, result.getItems().getFirst().getProductId());
        assertEquals("Pizza", result.getItems().getFirst().getTitle());
        assertEquals("pizza", result.getItems().getFirst().getSlug());
        assertEquals(new BigDecimal("2500.00"), result.getItems().getFirst().getPrice());
        assertTrue(result.getItems().getFirst().isInStock());
        assertEquals("pizza.jpg", result.getItems().getFirst().getMainImageKey());

        assertEquals(2L, result.getItems().get(1).getProductId());
        assertEquals("Burger", result.getItems().get(1).getTitle());
        assertFalse(result.getItems().get(1).isInStock());
        assertEquals("burger.jpg", result.getItems().get(1).getMainImageKey());

        assertEquals(2, result.getPageMeta().getTotalElements());
        assertEquals(1, result.getPageMeta().getTotalPages());
        assertFalse(result.getPageMeta().isHasNext());

        verify(productImageRepository)
                .findMainImages(List.of(1L, 2L));
    }

    @Test
    void getProducts_shouldCreateCorrectPageable() {
        ProductListQuery query = new ProductListQuery();

        query.setPage(3);
        query.setLimit(10);
        query.setSort(ProductSort.PRICE);
        query.setOrder(SortDirection.ASC);

        Page<Product> page = new PageImpl<>(
                List.of(),
                PageRequest.of(2, 10),
                0
        );

        when(productRepository.findAll(
                any(Specification.class),
                any(Pageable.class)
        )).thenReturn(page);

        productService.getProducts(query);

        ArgumentCaptor<Pageable> pageableCaptor =
                ArgumentCaptor.forClass(Pageable.class);

        verify(productRepository).findAll(
                any(Specification.class),
                pageableCaptor.capture()
        );

        Pageable pageable = pageableCaptor.getValue();

        assertEquals(2, pageable.getPageNumber());
        assertEquals(10, pageable.getPageSize());

        assertEquals(
                Sort.Direction.ASC,
                pageable.getSort().getOrderFor("price").getDirection()
        );
    }

    @Test
    void getProduct_shouldThrowResourceNotFoundException_whenProductDoesNotExist() {
        String slug = "pizza";

        when(productRepository.findActiveBySlug(slug))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> productService.getProduct(slug)
        );

        verify(productRepository).findActiveBySlug(slug);

        verifyNoInteractions(productImageRepository);
        verifyNoInteractions(categoryRepository);
    }

    @Test
    void getProduct_shouldReturnProductDetails_whenProductExists() {
        String slug = "pizza";

        Product product = mock(Product.class);
        ProductImage image = mock(ProductImage.class);
        Category category = mock(Category.class);

        when(productRepository.findActiveBySlug(slug))
                .thenReturn(Optional.of(product));

        when(product.getProductId()).thenReturn(1L);
        when(product.getTitle()).thenReturn("Pizza");
        when(product.getSlug()).thenReturn("pizza");
        when(product.getDescription()).thenReturn("Very tasty pizza");
        when(product.getPrice()).thenReturn(new BigDecimal("2500.00"));
        when(product.getStock()).thenReturn(10);

        when(productImageRepository.findAllByProductId(1L))
                .thenReturn(List.of(image));

        when(image.getImageKey()).thenReturn("pizza.jpg");
        when(image.getPosition()).thenReturn(0);

        when(categoryRepository.findActiveByProductId(1L))
                .thenReturn(List.of(category));

        when(category.getCategoryId()).thenReturn(5L);
        when(category.getName()).thenReturn("Food");
        when(category.getSlug()).thenReturn("food");

        ProductDetailsResponse result = productService.getProduct(slug);

        assertEquals(1L, result.getProductId());
        assertEquals("Pizza", result.getTitle());
        assertEquals("pizza", result.getSlug());
        assertEquals("Very tasty pizza", result.getDescription());
        assertEquals(new BigDecimal("2500.00"), result.getPrice());
        assertTrue(result.isInStock());

        assertEquals(1, result.getImages().size());
        assertEquals("pizza.jpg", result.getImages().getFirst().getImageKey());

        assertEquals(1, result.getCategories().size());
        assertEquals("Food", result.getCategories().getFirst().getName());
    }
}