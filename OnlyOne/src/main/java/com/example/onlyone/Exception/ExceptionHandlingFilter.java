package com.example.onlyone.Exception;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Slf4j
public class ExceptionHandlingFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        try {
            log.debug("Before filter chain execution: {}", request.getRequestURI());
            filterChain.doFilter(request, response);
            log.debug("After filter chain execution: {}", request.getRequestURI());
        } catch (Exception e) {
            log.error("=== Global Filter Chain Exception ===");
            log.error("URL: {} {}", request.getMethod(), request.getRequestURI());
            log.error("Exception: ", e);
            // 可以返回统一的错误响应
            handleException(response, e);
        }
    }

    private void handleException(HttpServletResponse response, Exception e) throws IOException {
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        response.setContentType("application/json");
        String errorMessage = "{\"error\": \"Internal Server Error\", \"message\": \"" + e.getMessage() + "\"}";
        response.getWriter().write(errorMessage);
    }

}
