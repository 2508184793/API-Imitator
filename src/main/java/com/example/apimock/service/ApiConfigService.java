package com.example.apimock.service;

import com.example.apimock.dto.ApiConfigRequest;
import com.example.apimock.dto.ApiConfigResponse;
import com.example.apimock.dto.FieldConfigDto;
import com.example.apimock.entity.ApiConfig;
import com.example.apimock.entity.FieldConfig;
import com.example.apimock.entity.FieldType;
import com.example.apimock.repository.ApiConfigRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ApiConfigService {

    private final ApiConfigRepository apiConfigRepository;

    public ApiConfigService(ApiConfigRepository apiConfigRepository) {
        this.apiConfigRepository = apiConfigRepository;
    }

    public List<ApiConfigResponse> findAll() {
        return apiConfigRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<ApiConfig> findAllEntities() {
        return apiConfigRepository.findAll();
    }

    public Optional<ApiConfigResponse> findById(Long id) {
        return apiConfigRepository.findById(id)
                .map(this::toResponse);
    }

    public Optional<ApiConfig> findByPathAndMethod(String path, String method) {
        return apiConfigRepository.findByPathAndMethodWithFields(path, method);
    }

    @Transactional
    public ApiConfigResponse create(ApiConfigRequest request) {
        ApiConfig apiConfig = new ApiConfig();
        apiConfig.setPath(request.getPath());
        apiConfig.setMethod(request.getMethod().toUpperCase());
        apiConfig.setDescription(request.getDescription());

        for (FieldConfigDto fieldDto : request.getFields()) {
            FieldConfig field = toEntity(fieldDto, apiConfig, null);
            apiConfig.addField(field);
        }

        ApiConfig saved = apiConfigRepository.save(apiConfig);
        return toResponse(saved);
    }

    @Transactional
    public Optional<ApiConfigResponse> update(Long id, ApiConfigRequest request) {
        return apiConfigRepository.findById(id)
                .map(apiConfig -> {
                    apiConfig.setPath(request.getPath());
                    apiConfig.setMethod(request.getMethod().toUpperCase());
                    apiConfig.setDescription(request.getDescription());

                    // 清除旧字段
                    apiConfig.getFields().clear();

                    // 添加新字段
                    for (FieldConfigDto fieldDto : request.getFields()) {
                        FieldConfig field = toEntity(fieldDto, apiConfig, null);
                        apiConfig.addField(field);
                    }

                    return toResponse(apiConfigRepository.save(apiConfig));
                });
    }

    @Transactional
    public boolean delete(Long id) {
        if (apiConfigRepository.existsById(id)) {
            apiConfigRepository.deleteById(id);
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
