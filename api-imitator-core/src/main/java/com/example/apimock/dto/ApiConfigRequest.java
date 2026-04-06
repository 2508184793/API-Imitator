package com.example.apimock.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class ApiConfigRequest {

    @NotBlank(message = "路径不能为空")
    private String path;

    @NotBlank(message = "HTTP 方法不能为空")
    private String method;

    private String description;

    @NotNull(message = "字段配置不能为空")
    @Valid
    private List<FieldConfigDto> fields;

    public ApiConfigRequest() {}

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public List<FieldConfigDto> getFields() { return fields; }
    public void setFields(List<FieldConfigDto> fields) { this.fields = fields; }
}
