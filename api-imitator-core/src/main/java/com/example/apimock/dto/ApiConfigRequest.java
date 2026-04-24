package com.example.apimock.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;

public class ApiConfigRequest {

    @NotBlank(message = "路径不能为空")
    private String path;

    @NotBlank(message = "HTTP 方法不能为空")
    private String method;

    private String description;

    // ========== v2 新增字段 ==========
    @Min(value = 100, message = "HTTP 状态码最小为 100")
    @Max(value = 599, message = "HTTP 状态码最大为 599")
    private Integer statusCode = 200;

    private Map<String, String> responseHeaders;

    @Min(value = 0, message = "延迟时间不能为负数")
    @Max(value = 30000, message = "延迟时间最大为 30000ms")
    private Integer delayMs = 0;

    private Boolean enabled = true;

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

    public Integer getStatusCode() { return statusCode; }
    public void setStatusCode(Integer statusCode) { this.statusCode = statusCode; }

    public Map<String, String> getResponseHeaders() { return responseHeaders; }
    public void setResponseHeaders(Map<String, String> responseHeaders) { this.responseHeaders = responseHeaders; }

    public Integer getDelayMs() { return delayMs; }
    public void setDelayMs(Integer delayMs) { this.delayMs = delayMs; }

    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }

    public List<FieldConfigDto> getFields() { return fields; }
    public void setFields(List<FieldConfigDto> fields) { this.fields = fields; }
}
