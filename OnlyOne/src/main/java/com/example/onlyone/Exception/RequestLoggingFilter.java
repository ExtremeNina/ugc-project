package com.example.onlyone.Exception;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;

@Component
@Order(1) // 在安全过滤器之前执行
public class RequestLoggingFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        long startTime = System.currentTimeMillis();

        log.info(">>> 请求开始: {} {}", httpRequest.getMethod(), httpRequest.getRequestURI());
        log.info("    查询参数: {}", httpRequest.getQueryString());
        log.info("    Content-Type: {}", httpRequest.getContentType());

        // 包装响应以捕获状态码
        ContentCachingResponseWrapper responseWrapper =
                new ContentCachingResponseWrapper(httpResponse);

        try {
            chain.doFilter(request, responseWrapper);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            log.info("<<< 请求结束: 状态码={}, 耗时={}ms",
                    responseWrapper.getStatus(), duration);

            responseWrapper.copyBodyToResponse();
        }
    }
}
