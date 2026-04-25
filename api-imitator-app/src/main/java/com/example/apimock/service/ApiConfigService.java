package com.example.apimock.service;

import com.example.apimock.dto.ApiConfigRequest;
import com.example.apimock.dto.ApiConfigResponse;
import com.example.apimock.dto.FieldConfigDto;
import com.example.apimock.entity.ApiConfig;
import com.example.apimock.entity.FieldConfig;
import com.example.apimock.entity.FieldType;
import com.example.apimock.repository.ApiConfigRepository;
import org.hibernate.Hibernate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class ApiConfigService {

    private static final Logger log = LoggerFactory.getLogger(ApiConfigService.class);
    private final ApiConfigRepository apiConfigRepository;
    private final ResponseBuilder responseBuilder;

    public ApiConfigService(ApiConfigRepository apiConfigRepository, ResponseBuilder responseBuilder) {
        this.apiConfigRepository = apiConfigRepository;
        this.responseBuilder = responseBuilder;
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "apiConfigs", key = "'all'")
    public List<ApiConfigResponse> findAll() {
        log.debug("查询所有 API 配置");
        return apiConfigRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * 在事务内查找匹配配置并构建响应
     * 解决 Hibernate 懒加载和字段值丢失问题
     */
    @Transactional(readOnly = true)
    public Optional<String> findAndBuildResponse(String requestPath, String requestMethod, Map<String,String> params) {
        // 在事务内查询所有配置
        List<ApiConfig> configs = apiConfigRepository.findAll();
        
        for (ApiConfig config : configs) {
            // 检查路径匹配
            if (pathMatches(config.getPath(), requestPath, requestMethod, config.getMethod(), params)) {
                // 在事务内初始化所有字段
                Hibernate.initialize(config.getFields());
                initializeFields(config.getFields());
                // 在事务内直接构建响应
                return Optional.ofNullable(responseBuilder.buildResponse(config, params));
            }
        }
        return Optional.empty();
    }
    
    /**
     * 路径匹配逻辑（从 Controller 移到 Service，保证在事务内）
     */
    private boolean pathMatches(String configPath, String requestPath, String requestMethod, String configMethod, Map<String,String> params) {
        // 方法必须匹配
        if (!configMethod.equalsIgnoreCase(requestMethod)) {
            return false;
        }
        
        // 精确路径匹配
        if (configPath.equals(requestPath)) {
            return true;
        }
        
        // 路径参数匹配：如 /user/{id} 匹配 /user/123
        String[] configParts = configPath.split("/");
        String[] requestParts = requestPath.split("/");
        
        if (configParts.length != requestParts.length) {
            return false;
        }
        
        for (int i = 0; i < configParts.length; i++) {
            String configPart = configParts[i];
            String requestPart = requestParts[i];
            
            if (configPart.startsWith("{") && configPart.endsWith("}")) {
                // 这是一个路径参数
                String paramName = configPart.substring(1, configPart.length() - 1);
                params.put(paramName, requestPart);
            } else if (!configPart.equals(requestPart)) {
                return false;
            }
        }
        
        return true;
    }

    private void initializeFields(java.util.Collection<FieldConfig> fields) {
        if (fields == null || fields.isEmpty()) {
            return;
        }
        for (FieldConfig field : fields) {
            // 强制 Hibernate 初始化所有字段
            Hibernate.initialize(field);
            String val = field.getFieldValue(); // 强制读取
            log.info("初始化字段: {} = {}", field.getFieldName(), val);
            // 初始化子项
            if (field.getChildren() != null) {
                Hibernate.initialize(field.getChildren());
                initializeFields(field.getChildren());
            }
        }
    }

    @Cacheable(value = "apiConfigs", key = "#id")
    public Optional<ApiConfigResponse> findById(Long id) {
        log.debug("查询 API 配置: id={}", id);
        return apiConfigRepository.findById(id)
                .map(this::toResponse);
    }

    public Optional<ApiConfig> findByPathAndMethod(String path, String method) {
        return apiConfigRepository.findByPathAndMethodWithFields(path, method);
    }

    @Transactional
    @CacheEvict(value = {"apiConfigs", "apiConfigEntities"}, allEntries = true)
    public ApiConfigResponse create(ApiConfigRequest request) {
        log.info("创建新的 API 配置: {} {}", request.getMethod(), request.getPath());
        ApiConfig apiConfig = new ApiConfig();
        apiConfig.setPath(request.getPath());
        apiConfig.setMethod(request.getMethod().toUpperCase());
        apiConfig.setDescription(request.getDescription());

        // v2 新字段
        if (request.getStatusCode() != null) {
            apiConfig.setStatusCode(request.getStatusCode());
        }
        if (request.getDelayMs() != null) {
            apiConfig.setDelayMs(request.getDelayMs());
        }
        if (request.getEnabled() != null) {
            apiConfig.setEnabled(request.getEnabled());
        }
        apiConfig.setResponseHeaders(responseBuilder.serializeResponseHeaders(request.getResponseHeaders()));

        for (FieldConfigDto fieldDto : request.getFields()) {
            FieldConfig field = toEntity(fieldDto, apiConfig, null);
            apiConfig.addField(field);
        }

        ApiConfig saved = apiConfigRepository.save(apiConfig);
        responseBuilder.clearCache();
        return toResponse(saved);
    }

    @Transactional
    @CacheEvict(value = {"apiConfigs", "apiConfigEntities"}, allEntries = true)
    public Optional<ApiConfigResponse> update(Long id, ApiConfigRequest request) {
        log.info("更新 API 配置: id={}", id);
        return apiConfigRepository.findById(id)
                .map(apiConfig -> {
                    apiConfig.setPath(request.getPath());
                    apiConfig.setMethod(request.getMethod().toUpperCase());
                    apiConfig.setDescription(request.getDescription());

                    // v2 新字段
                    if (request.getStatusCode() != null) {
                        apiConfig.setStatusCode(request.getStatusCode());
                    }
                    if (request.getDelayMs() != null) {
                        apiConfig.setDelayMs(request.getDelayMs());
                    }
                    if (request.getEnabled() != null) {
                        apiConfig.setEnabled(request.getEnabled());
                    }
                    apiConfig.setResponseHeaders(responseBuilder.serializeResponseHeaders(request.getResponseHeaders()));

                    // 清除旧字段
                    apiConfig.getFields().clear();

                    // 添加新字段
                    for (FieldConfigDto fieldDto : request.getFields()) {
                        FieldConfig field = toEntity(fieldDto, apiConfig, null);
                        apiConfig.addField(field);
                    }

                    ApiConfigResponse response = toResponse(apiConfigRepository.save(apiConfig));
                    responseBuilder.clearCache();
                    return response;
                });
    }

    @Transactional
    @CacheEvict(value = {"apiConfigs", "apiConfigEntities"}, allEntries = true)
    public boolean delete(Long id) {
        log.info("删除 API 配置: id={}", id);
        if (apiConfigRepository.existsById(id)) {
            apiConfigRepository.deleteById(id);
            responseBuilder.clearCache();
            return true;
        }
        return false;
    }

    private FieldConfig toEntity(FieldConfigDto dto, ApiConfig apiConfig, FieldConfig parent) {
        FieldConfig field = new FieldConfig();
        field.setFieldName(dto.getName());
        field.setFieldType(dto.getType());
        field.setApiConfig(apiConfig);
        field.setParentField(parent);

        // 处理 ARRAY 类型的 arrayItems
        if (dto.getType() == FieldType.ARRAY && dto.getArrayItems() != null && !dto.getArrayItems().isEmpty()) {
            // 判断是否是简单的逗号分隔值（基本类型数组）还是 JSON 对象数组
            String firstValue = dto.getArrayItems().get(0).getValue();
            boolean hasJsonObjects = firstValue != null && (firstValue.trim().startsWith("{") || firstValue.trim().startsWith("["));

            if (hasJsonObjects) {
                // JSON 对象数组：为每个项创建子节点
                for (FieldConfigDto.ArrayItemDto item : dto.getArrayItems()) {
                    FieldConfig itemChild = new FieldConfig();
                    itemChild.setFieldName("item");
                    itemChild.setFieldType(FieldType.STRING);
                    itemChild.setFieldValue(item.getValue());
                    itemChild.setApiConfig(apiConfig);
                    itemChild.setParentField(field);
                    field.getChildren().add(itemChild);
                }
            } else {
                // 基本类型数组：用逗号分隔存储
                String arrayValue = dto.getArrayItems().stream()
                        .map(FieldConfigDto.ArrayItemDto::getValue)
                        .collect(Collectors.joining(","));
                field.setFieldValue(arrayValue);
            }
        } else {
            field.setFieldValue(dto.getValue());
            if (dto.getChildren() != null) {
                for (FieldConfigDto childDto : dto.getChildren()) {
                    FieldConfig child = toEntity(childDto, apiConfig, field);
                    field.getChildren().add(child);
                }
            }
        }
        return field;
    }

    private ApiConfigResponse toResponse(ApiConfig apiConfig) {
        ApiConfigResponse response = new ApiConfigResponse();
        response.setId(apiConfig.getId());
        response.setPath(apiConfig.getPath());
        response.setMethod(apiConfig.getMethod());
        response.setDescription(apiConfig.getDescription());
        response.setStatusCode(apiConfig.getStatusCode());
        response.setResponseHeaders(responseBuilder.parseResponseHeaders(apiConfig.getResponseHeaders()));
        response.setDelayMs(apiConfig.getDelayMs());
        response.setEnabled(apiConfig.getEnabled());
        response.setCreatedAt(apiConfig.getCreatedAt());
        response.setUpdatedAt(apiConfig.getUpdatedAt());
        response.setFields(toFieldDtos(apiConfig.getFields(), null));
        return response;
    }

    private List<FieldConfigDto> toFieldDtos(Collection<FieldConfig> fields, FieldConfig parent) {
        return fields.stream()
                .filter(f -> f.getParentField() == parent)
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    private FieldConfigDto toDto(FieldConfig field) {
        FieldConfigDto dto = new FieldConfigDto();
        dto.setId(field.getId());
        dto.setName(field.getFieldName());
        dto.setType(field.getFieldType());
        dto.setValue(field.getFieldValue());
        dto.setChildren(toFieldDtos(field.getChildren(), field));
        return dto;
    }
}
