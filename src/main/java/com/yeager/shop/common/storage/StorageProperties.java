package com.yeager.shop.common.storage;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "storage")
public class StorageProperties {
    private String endpoint;
    private String publicUrl;

    private String accessKey;
    private String secretKey;

    private String region;
    private String bucket;
}
