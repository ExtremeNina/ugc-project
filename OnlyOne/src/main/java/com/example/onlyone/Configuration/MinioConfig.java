package com.example.onlyone.Configuration;

import com.example.onlyone.Properties.MinioProperties;
import io.minio.MinioClient;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class MinioConfig {


    @Resource
    private MinioProperties minioProperties;

    @Bean
    public MinioClient minioClient() {

        return MinioClient.builder()
                .endpoint(minioProperties.getEndpoint())
                .credentials(minioProperties.getAccessKey(),minioProperties.getSecretKey())
                .build();
    }


}
