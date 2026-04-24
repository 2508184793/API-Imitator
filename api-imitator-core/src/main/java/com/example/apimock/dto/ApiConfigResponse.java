package com.example.apimock.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class ApiConfigResponse {

    private Long id;
    private String path;
    private String method;
    private String description;
    private Integer statusCode;
    private Map<String, String> responseHeaders;
    private Integer delayMs;
    private Boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<FieldConfigDto> fields;

    public ApiConfigResponse() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

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

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public List<FieldConfigDto> getFields() { return fields; }
    public void setFields(List<FieldConfigDto> fields) { this.fields = fields; }
}
