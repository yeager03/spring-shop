package com.yeager.shop.catalog.controller;

import com.yeager.shop.catalog.dto.*;
import com.yeager.shop.catalog.service.ProductService;
import com.yeager.shop.common.dto.PageMeta;
import com.yeager.shop.common.dto.PagedResponse;
import com.yeager.shop.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProductControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;

    @Test
    void getProduct_shouldReturn400_whenSlugIsInvalid() throws Exception {
        String invalidSlug = "INVALID!!!";

        mockMvc.perform(get("/products/{slug}", invalidSlug))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.detail")
                        .value("One or more request parameters are invalid"))
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors[0].field").value("slug"));

        verifyNoInteractions(productService);
    }

    @Test
    void getProduct_shouldReturn404_whenProductDoesNotExist() throws Exception {
        String slug = "pizza";

        when(productService.getProduct(slug))
                .thenThrow(new ResourceNotFoundException("Product not found by slug: " + slug));

        mockMvc.perform(get("/products/{slug}", slug))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource not found"))
                .andExpect(jsonPath("$.detail").value("Product not found by slug: pizza"));
    }

    @Test
    void getProduct_shouldReturn200AndProduct_whenProductExists() throws Exception {
        String slug = "pizza";

        ProductDetailsResponse response = new ProductDetailsResponse(
                1L,
                "Pizza",
                "pizza",
                "Very tasty pizza",
                new BigDecimal("2500.00"),
                true,
                List.of(
                        new ProductImageResponse(
                                1L,
                                "pizza-main",
                                "pizza-main.jpg",
                                0
                        )
                ),
                List.of(
                        new CategoryResponse(
                                5L,
                                "Food",
                                "food"
                        )
                )
        );

        when(productService.getProduct(slug))
                .thenReturn(response);

        mockMvc.perform(get("/products/{slug}", slug))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(1))
                .andExpect(jsonPath("$.title").value("Pizza"))
                .andExpect(jsonPath("$.slug").value("pizza"))
                .andExpect(jsonPath("$.description").value("Very tasty pizza"))
                .andExpect(jsonPath("$.inStock").value(true))
                .andExpect(jsonPath("$.images.length()").value(1))
                .andExpect(jsonPath("$.images[0].imageUrl").value("pizza-main.jpg"))
                .andExpect(jsonPath("$.images[0].position").value(0))
                .andExpect(jsonPath("$.categories.length()").value(1))
                .andExpect(jsonPath("$.categories[0].categoryId").value(5))
                .andExpect(jsonPath("$.categories[0].name").value("Food"))
                .andExpect(jsonPath("$.categories[0].slug").value("food"));

        verify(productService).getProduct(slug);
    }

    @Test
    void getProducts_shouldReturn400_whenPageIsLessThanOne() throws Exception {
        mockMvc.perform(
                        get("/products")
                                .param("page", "0")
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.detail")
                        .value("One or more request fields are invalid"))
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors[0].field").value("page"));

        verifyNoInteractions(productService);
    }

    @Test
    void getProducts_shouldReturn400_whenSortIsInvalid() throws Exception {
        mockMvc.perform(
                        get("/products")
                                .param("sort", "abracadabra")
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.detail")
                        .value("One or more request fields are invalid"))
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors[0].field").value("sort"));

        verifyNoInteractions(productService);
    }

    @Test
    void getProducts_shouldBindQueryParametersAndReturn200() throws Exception {
        PagedResponse<ProductListItemResponse> response = new PagedResponse<>(
                List.of(),
                new PageMeta(
                        2,
                        10,
                        0,
                        0,
                        false
                ),
                Map.of(
                        "search", "pizza",
                        "sort", "price",
                        "order", "asc"
                )
        );

        when(productService.getProducts(any(ProductListQuery.class)))
                .thenReturn(response);

        mockMvc.perform(
                        get("/products")
                                .param("page", "2")
                                .param("limit", "10")
                                .param("search", "pizza")
                                .param("sort", "price")
                                .param("order", "asc")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.pageMeta.page").value(2))
                .andExpect(jsonPath("$.pageMeta.limit").value(10));

        ArgumentCaptor<ProductListQuery> queryCaptor =
                ArgumentCaptor.forClass(ProductListQuery.class);

        verify(productService)
                .getProducts(queryCaptor.capture());

        ProductListQuery query = queryCaptor.getValue();

        assertEquals(2, query.getPage());
        assertEquals(10, query.getLimit());
        assertEquals("pizza", query.getSearch());
        assertEquals(ProductSort.PRICE, query.getSort());
        assertEquals(SortDirection.ASC, query.getOrder());
    }
}