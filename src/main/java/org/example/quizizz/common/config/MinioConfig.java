package org.example.quizizz.common.config;

import io.minio.MinioClient;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Slf4j
@Configuration
@ConfigurationProperties(prefix = "minio")
@Data
public class MinioConfig {

    private String endpoint;
    private String publicEndpoint;
    private String accessKey;
    private String secretKey;
    private String imageBucket;
    private String avatarBucket;
    private Integer presignedUrlExpiryDays = 7;
    private Integer presignedUrlExpiryMinutes = 60;

    /**
     * MinIO client cho thao tác nội bộ (upload, delete).
     */
    @Bean
    @Primary
    public MinioClient internalMinioClient() {
        try {
            log.info("Configuring internal MinIO client with endpoint: {}", endpoint);
            return MinioClient.builder()
                    .endpoint(endpoint)
                    .credentials(accessKey, secretKey)
                    .region("us-east-1")
                    .build();
        } catch (Exception e) {
            log.error("Failed to configure internal MinIO client: {}", e.getMessage(), e);
            throw new RuntimeException("MinIO configuration failed", e);
        }
    }

    /**
     * MinIO client cho presigned URL (sử dụng public endpoint).
     */
    @Bean
    @Qualifier("publicMinioClient")
    public MinioClient publicMinioClient() {
        try {
            log.info("Configuring public MinIO client with endpoint: {}", publicEndpoint);
            return MinioClient.builder()
                    .endpoint(publicEndpoint)
                    .credentials(accessKey, secretKey)
                    .region("us-east-1")
                    .build();
        } catch (Exception e) {
            log.error("Failed to configure public MinIO client: {}", e.getMessage(), e);
            throw new RuntimeException("MinIO public client configuration failed", e);
        }
    }
}
