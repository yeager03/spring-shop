package com.yeager.shop.common.storage;

import com.yeager.shop.common.exception.StorageException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.io.InputStream;

@Service
@RequiredArgsConstructor
public class StorageService {
    private final S3Client s3Client;
    private final StorageProperties storageProperties;

    public void upload(String key, MultipartFile file) {
        PutObjectRequest request = PutObjectRequest
                .builder()
                .bucket(storageProperties.getBucket())
                .key(key)
                .contentType(file.getContentType())
                .contentLength(file.getSize())
                .build();

        try (InputStream inputStream = file.getInputStream()) {
            s3Client.putObject(
                    request,
                    RequestBody.fromInputStream(
                            inputStream,
                            file.getSize()
                    )
            );
        } catch (IOException | SdkException exception) {
            throw new StorageException(
                    "Failed to upload file",
                    exception
            );
        }
    }

    public void delete(String key) {
        DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(storageProperties.getBucket())
                .key(key)
                .build();

        try {
            s3Client.deleteObject(request);
        } catch (SdkException exception) {
            throw new StorageException(
                    "Failed to delete file",
                    exception
            );
        }
    }

    public String getPublicUrl(String key) {
        return storageProperties.getPublicUrl()
                + "/"
                + storageProperties.getBucket()
                + "/"
                + key;
    }
}
