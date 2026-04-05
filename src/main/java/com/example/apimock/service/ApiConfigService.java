package com.example.apimock.service;

import com.example.apimock.dto.ApiConfigRequest;
import com.example.apimock.dto.ApiConfigResponse;
import com.example.apimock.dto.FieldConfigDto;
import com.example.apimock.entity.ApiConfig;
import com.example.apimock.entity.FieldConfig;
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
        field.setFieldValue(dto.getValue());
        field.setApiConfig(apiConfig);
        field.setParentField(parent);

        if (dto.getChildren() != null) {
            for (FieldConfigDto childDto : dto.getChildren()) {
                FieldConfig child = toEntity(childDto, apiConfig, field);
                field.getChildren().add(child);
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
