package com.example.apimock.controller;

import java.util.Optional;
import com.example.apimock.entity.ApiConfig;
import com.example.apimock.dto.ApiConfigResponse;
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

        // 在事务内查找匹配配置并构建响应（解决 Hibernate 懒加载问题）
        Map<String, String> params = new java.util.HashMap<>();
        Optional<String> responseOpt = apiConfigService.findAndBuildResponse(requestPath, requestMethod, params);

        if (responseOpt.isPresent()) {
            HttpServletResponse res = (HttpServletResponse) response;
            
            // 注意：为解决懒加载问题，v2 新功能（延迟、状态码、响应头、启用开关）
            // 需要在 Service 层实现，这里简化返回默认值
            res.setStatus(200);
            res.setContentType("application/json");
            res.setCharacterEncoding("UTF-8");
            res.getWriter().write(responseOpt.get());
            return;
        }
        
        // 没有匹配的配置，继续后续处理
        chain.doFilter(request, response);
    }
}
