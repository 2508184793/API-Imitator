package com.example.apimock.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "api_config")
public class ApiConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "path", nullable = false)
    private String path;

    @Column(name = "method", nullable = false)
    private String method;

    @Column(name = "description")
    private String description;

    // ========== v2 新增功能字段 ==========
    @Column(name = "status_code")
    private Integer statusCode = 200;

    @Column(name = "response_headers", length = 2000)
    private String responseHeaders;

    @Column(name = "delay_ms")
    private Integer delayMs = 0;

    @Column(name = "enabled")
    private Boolean enabled = true;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "apiConfig", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<FieldConfig> fields = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public ApiConfig() {}

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

    public String getResponseHeaders() { return responseHeaders; }
    public void setResponseHeaders(String responseHeaders) { this.responseHeaders = responseHeaders; }

    public Integer getDelayMs() { return delayMs; }
    public void setDelayMs(Integer delayMs) { this.delayMs = delayMs; }

    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public List<FieldConfig> getFields() { return fields; }
    public void setFields(List<FieldConfig> fields) { this.fields = fields; }

    public void addField(FieldConfig field) {
        fields.add(field);
        field.setApiConfig(this);
    }

    public void removeField(FieldConfig field) {
        fields.remove(field);
        field.setApiConfig(null);
    }
}
