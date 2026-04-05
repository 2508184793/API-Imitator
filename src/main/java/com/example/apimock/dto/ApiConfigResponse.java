package com.example.apimock.dto;

import java.time.LocalDateTime;
import java.util.List;

public class ApiConfigResponse {

    private Long id;
    private String path;
    private String method;
    private String description;
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

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public List<FieldConfigDto> getFields() { return fields; }
    public void setFields(List<FieldConfigDto> fields) { this.fields = fields; }
}
