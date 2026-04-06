package com.example.apimock.entity;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "field_config")
public class FieldConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "api_config_id", nullable = false)
    private ApiConfig apiConfig;

    @Column(name = "field_name")
    private String fieldName;

    @Enumerated(EnumType.STRING)
    @Column(name = "field_type", nullable = false)
    private FieldType fieldType;

    @Column(name = "field_value")
    private String fieldValue;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_field_id")
    private FieldConfig parentField;

    @OneToMany(mappedBy = "parentField", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private Set<FieldConfig> children = new HashSet<>();

    public FieldConfig() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public ApiConfig getApiConfig() { return apiConfig; }
    public void setApiConfig(ApiConfig apiConfig) { this.apiConfig = apiConfig; }

    public String getFieldName() { return fieldName; }
    public void setFieldName(String fieldName) { this.fieldName = fieldName; }

    public FieldType getFieldType() { return fieldType; }
    public void setFieldType(FieldType fieldType) { this.fieldType = fieldType; }

    public String getFieldValue() { return fieldValue; }
    public void setFieldValue(String fieldValue) { this.fieldValue = fieldValue; }

    public FieldConfig getParentField() { return parentField; }
    public void setParentField(FieldConfig parentField) { this.parentField = parentField; }

    public Set<FieldConfig> getChildren() { return children; }
    public void setChildren(Set<FieldConfig> children) { this.children = children; }
}
