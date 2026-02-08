package com.example.onlyone.Utils;


import cn.hutool.core.lang.UUID;
import com.example.onlyone.Properties.MinioProperties;
import io.minio.*;
import jakarta.annotation.Resource;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class MinioUtils {

    @Resource
    private MinioClient minioClient;

    @Resource
    private MinioProperties minioProperties;

    //判断存储桶是否存在
    @SneakyThrows
    public boolean bucketExists(String bucketName) {
        return minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
    }

    //创建存储桶
    @SneakyThrows
    public void createBucket(String bucketName) {
        if (!bucketExists(bucketName)) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
        }
    }

    //实现简单图片上传
    @SneakyThrows
    public Map<String,String> uploadFile(MultipartFile file, String bucketName) {

        //判断文件是否存在
        if(file == null || file.getSize() == 0 || file.isEmpty()) {
            return  null;
        }
        //判断桶是否存在
        if(!bucketExists(bucketName)) {
            createBucket(bucketName);
        }
        //确保当前桶是公共读取
        ensureBucketExistsAndPublic(bucketName);

        //获取原文件名
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String fileName = UUID.randomUUID().toString() + extension;

        //上传文件
        minioClient.putObject(PutObjectArgs.builder()
                .bucket(bucketName)
                .object(fileName)
                .contentType(file.getContentType())
                //暂时不支持分片上传
                .stream(file.getInputStream(), file.getSize(), -1)
                .build());

        Map<String, String> resultMap = new HashMap<>();
        resultMap.put("fileName", fileName);
        resultMap.put("originalFilename", originalFilename);
        resultMap.put("url",generatePermanentImageUrl(bucketName,fileName));

        return resultMap;
    }

    /**
     * 设置存储桶为公共读取
     */
    @SneakyThrows
    public void setBucketPublicPolicy(String bucketName) {
        String publicPolicy = String.format("""
    {
        "Version": "2012-10-17",
        "Statement": [
            {
                "Effect": "Allow",
                "Principal": "*",
                "Action": [
                    "s3:GetObject"
                ],
                "Resource": "arn:aws:s3:::%s/*"
            }
        ]
    }
    """, bucketName);

        minioClient.setBucketPolicy(SetBucketPolicyArgs.builder()
                .bucket(bucketName)
                .config(publicPolicy)
                .build());

        log.info("存储桶 {} 已设置为公共读取", bucketName);
    }

    /**
     * 确保存储桶存在且为公共读取
     */
    @SneakyThrows
    public void ensureBucketExistsAndPublic(String bucketName) {
        // 检查存储桶是否存在
        if (!bucketExists(bucketName)) {
            // 创建存储桶
            createBucket(bucketName);

            // 设置公共读取策略
            setBucketPublicPolicy(bucketName);
        } else {
            // 如果存储桶已存在，确保策略是公共读取
            try {
                String currentPolicy = minioClient.getBucketPolicy(GetBucketPolicyArgs.builder()
                        .bucket(bucketName)
                        .build());

                // 简单检查策略是否包含公共读取，如果没有则重新设置
                if (!currentPolicy.contains("\"Effect\":\"Allow\"") || !currentPolicy.contains("\"Principal\":\"*\"")) {
                    setBucketPublicPolicy(bucketName);
                }
            } catch (Exception e) {
                // 如果获取策略失败，可能是没有策略，直接设置
                setBucketPublicPolicy(bucketName);
            }
        }
    }

    /**
     * 生成永久图片URL
     */
    public String generatePermanentImageUrl(String bucketName, String fileName) {
        // 根据你的实际MinIO配置修改这里的地址
        String minioEndpoint = minioProperties.getEndpoint();
        return String.format("%s/%s/%s", minioEndpoint, bucketName, fileName);
    }

}
