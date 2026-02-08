package com.example.onlyone.Utils;


import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;


//验证工具类
@Component
public class ValidationUtils {

    /**
     * 验证文章标题
     */
    public static void validateArticleTitle(String title) {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("文章标题不能为空");
        }
    }

    /**
     * 验证日期格式
     */
    public static String validateAndProcessDate(String date) {
        if (date == null || date.trim().isEmpty()) {
            return LocalDate.now().toString();
        }

        try {
            LocalDate parsedDate = LocalDate.parse(date);
            return parsedDate.toString();
        } catch (Exception e) {
            throw new IllegalArgumentException("日期格式不正确，请使用 yyyy-MM-dd 格式");
        }
    }

    /**
     * 验证图片文件
     */
    public static void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("图片文件不能为空");
        }

        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename != null ?
                originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase() : null;

        List<String> allowedExtensions = Arrays.asList(".jpg", ".jpeg", ".png", ".gif", ".webp");

        if (!allowedExtensions.contains(extension)) {
            throw new IllegalArgumentException("只支持以下图片格式: " + String.join(", ", allowedExtensions));
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("文件类型不是图片");
        }
    }

}
