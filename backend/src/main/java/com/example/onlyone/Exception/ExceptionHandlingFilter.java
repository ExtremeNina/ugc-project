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
            handleException(response, e);
        } catch (Throwable t) {
            log.error("=== Global Filter Chain Error ===");
            log.error("URL: {} {}", request.getMethod(), request.getRequestURI());
            log.error("Error: ", t);
            handleError(response, t);
        }
    }

    private void handleException(HttpServletResponse response, Exception e) throws IOException {
        writeErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
    }

    private void handleError(HttpServletResponse response, Throwable t) throws IOException {
        writeErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "服务器内部错误，请稍后重试");
    }

    private void writeErrorResponse(HttpServletResponse response, int status, String message) throws IOException {
        if (response.isCommitted()) {
            return;
        }
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        String errorJson = "{\"code\":" + status + ",\"msg\":\"" + message + "\",\"data\":null}";
        response.getWriter().write(errorJson);
    }

}
