package com.example.apimock.controller;

import com.example.apimock.entity.ApiConfig;
import com.example.apimock.service.ApiConfigService;
import com.example.apimock.service.ResponseBuilder;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

/**
 * v2 动态 API 控制器
 * 新增功能：
 * - 自定义 HTTP 状态码
 * - 自定义响应头
 * - 延迟响应（模拟网络）
 * - 配置启用/禁用开关
 * - 性能优化：缓存机制
 */
@Component
public class DynamicApiController implements Filter {

    private static final Logger log = LoggerFactory.getLogger(DynamicApiController.class);
    private final ApiConfigService apiConfigService;
    private final ResponseBuilder responseBuilder;

    public DynamicApiController(ApiConfigService apiConfigService, ResponseBuilder responseBuilder) {
        this.apiConfigService = apiConfigService;
        this.responseBuilder = responseBuilder;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        String requestPath = req.getRequestURI();
        String requestMethod = req.getMethod();

        // 不要拦截基础的静态文件和后台配置接口
        if (requestPath.startsWith("/api/configs") || requestPath.equals("/") || 
            requestPath.endsWith(".html") || requestPath.endsWith(".js") || 
            requestPath.endsWith(".css") || requestPath.endsWith(".ico") ||
            requestPath.endsWith(".woff2")) {
            chain.doFilter(request, response);
            return;
        }

        // 查找匹配的 API 配置
        ApiConfig matchedConfig = findMatchingApiConfig(requestPath, requestMethod);

        if (matchedConfig != null) {
            // v2 新功能：检查配置是否启用
            if (matchedConfig.getEnabled() == null || !matchedConfig.getEnabled()) {
                log.debug("API 配置已禁用: {} {}", requestMethod, requestPath);
                chain.doFilter(request, response);
                return;
            }

            // v2 新功能：延迟响应
            if (matchedConfig.getDelayMs() != null && matchedConfig.getDelayMs() > 0) {
                try {
                    Thread.sleep(matchedConfig.getDelayMs());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            // 构建响应
            Map<String, String> pathParams = responseBuilder.extractPathParams(matchedConfig.getPath(), requestPath);
            String respBody = responseBuilder.buildResponse(matchedConfig, pathParams);

            HttpServletResponse res = (HttpServletResponse) response;

            // v2 新功能：自定义状态码
            int statusCode = matchedConfig.getStatusCode() != null ? matchedConfig.getStatusCode() : 200;
            res.setStatus(statusCode);

            // v2 新功能：自定义响应头
            Map<String, String> customHeaders = responseBuilder.parseResponseHeaders(matchedConfig.getResponseHeaders());
            for (Map.Entry<String, String> entry : customHeaders.entrySet()) {
                res.setHeader(entry.getKey(), entry.getValue());
            }

            // 默认 Content-Type
            if (!customHeaders.containsKey("Content-Type") && !customHeaders.containsKey("content-type")) {
                res.setContentType("application/json;charset=UTF-8");
            }

            // 记录访问日志
            log.info("动态 API 响应: {} {} -> {} ({}ms)", requestMethod, requestPath, statusCode, matchedConfig.getDelayMs());

            res.getWriter().write(respBody);
            return;
        }

        // 没有匹配的配置，继续后续处理
        chain.doFilter(request, response);
    }

    /**
     * 查找匹配的 API 配置
     * v2 优化：使用缓存的配置列表
     */
    private ApiConfig findMatchingApiConfig(String requestPath, String requestMethod) {
        for (ApiConfig config : apiConfigService.findAllEntities()) {
            if (config.getMethod().equalsIgnoreCase(requestMethod)) {
                String matchedPath = responseBuilder.matchPath(config.getPath(), requestPath);
                if (matchedPath != null) {
                    return config;
                }
            }
        }
        return null;
    }
}
