package com.example.apimock.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * v2 缓存配置
 * 使用 Spring Cache 优化 API 配置查询性能
 */
@Configuration
@EnableCaching
public class CacheConfig {

    private static final Logger log = LoggerFactory.getLogger(CacheConfig.class);

    @Bean
    public CacheManager cacheManager() {
        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager(
            "apiConfigs", 
            "apiConfigEntities"
        );
        log.info("Spring Cache 已启用，缓存名称: apiConfigs, apiConfigEntities");
        return cacheManager;
    }
}