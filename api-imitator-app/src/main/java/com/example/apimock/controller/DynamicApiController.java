package com.example.apimock.controller;

import com.example.apimock.entity.ApiConfig;
import com.example.apimock.service.ApiConfigService;
import com.example.apimock.service.ResponseBuilder;
import org.springframework.transaction.support.TransactionTemplate;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Component
public class DynamicApiController implements Filter {

    private final ApiConfigService apiConfigService;
    private final ResponseBuilder responseBuilder;
    private final TransactionTemplate transactionTemplate;

    public DynamicApiController(ApiConfigService apiConfigService, ResponseBuilder responseBuilder, TransactionTemplate transactionTemplate) {
        this.apiConfigService = apiConfigService;
        this.responseBuilder = responseBuilder;
        this.transactionTemplate = transactionTemplate;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        String requestPath = req.getRequestURI();
        String requestMethod = req.getMethod();

        // 不要拦截基础的静态文件和后台配置接口
        if (requestPath.startsWith("/api/configs") || requestPath.equals("/") || 
            requestPath.endsWith(".html") || requestPath.endsWith(".js") || requestPath.endsWith(".css") || requestPath.endsWith(".ico")) {
            chain.doFilter(request, response);
            return;
        }

        Boolean handled = transactionTemplate.execute(status -> {
            ApiConfig apiConfig = findMatchingApiConfig(requestPath, requestMethod);
            if (apiConfig != null) {
                Map<String, String> pathParams = extractPathParams(apiConfig.getPath(), requestPath);
                String respBody = responseBuilder.buildResponse(apiConfig, pathParams);
                
                HttpServletResponse res = (HttpServletResponse) response;
                res.setStatus(200);
                res.setContentType("application/json;charset=UTF-8");
                try {
                    res.getWriter().write(respBody);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                return true;
            }
            return false;
        });

        if (handled != null && handled) {
            return;
        }

        chain.doFilter(request, response);
    }

    private ApiConfig findMatchingApiConfig(String requestPath, String requestMethod) {
        for (ApiConfig config : apiConfigService.findAllEntities()) {
            if (config.getMethod().equalsIgnoreCase(requestMethod)) {
                String matchedPath = responseBuilder.matchPath(config.getPath(), requestPath);
                if (matchedPath != null) {
                    return apiConfigService.findByPathAndMethod(config.getPath(), config.getMethod())
                            .orElse(null);
                }
            }
        }
        return null;
    }

    private Map<String, String> extractPathParams(String configuredPath, String requestPath) {
        return responseBuilder.extractPathParams(configuredPath, requestPath);
    }
}
