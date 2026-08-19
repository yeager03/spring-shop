package com.yeager.shop.catalog.converter;

import com.yeager.shop.catalog.dto.ProductSort;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class ProductSortConverter implements Converter<String, ProductSort> {
    @Override
    public ProductSort convert(String source) {
        return switch (source.toLowerCase(Locale.ROOT)) {
            case "title" -> ProductSort.TITLE;
            case "price" -> ProductSort.PRICE;
            case "created_at" -> ProductSort.CREATED_AT;
            default -> throw new IllegalArgumentException(
                    "Unsupported product sort: " + source
            );
        };
    }
}
