package com.yeager.shop.catalog.repository.projection;

public interface CategoryTreeProjection {
    Long getCategoryId();

    Long getParentId();

    String getName();

    String getSlug();

    Integer getPosition();
}
