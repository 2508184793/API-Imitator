package com.example.apimock.service;

import com.example.apimock.entity.ApiConfig;
import com.example.apimock.entity.FieldConfig;
import com.example.apimock.entity.FieldType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ResponseBuilder {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final Pattern PATH_PARAM_PATTERN = Pattern.compile("\\{([^}]+)\\}");

    public String buildResponse(ApiConfig apiConfig, Map<String, String> pathParams) {
        ObjectNode root = objectMapper.createObjectNode();

        for (FieldConfig field : apiConfig.getFields()) {
            if (field.getParentField() == null) {
                buildField(field, root, pathParams);
            }
        }

        try {
            return objectMapper.writeValueAsString(root);
        } catch (Exception e) {
            throw new RuntimeException("Failed to build response", e);
        }
    }

    private void buildField(FieldConfig field, ObjectNode parent, Map<String, String> pathParams) {
        String name = field.getFieldName();
        String value = resolveValue(field.getFieldValue(), pathParams);

        switch (field.getFieldType()) {
            case STRING:
                parent.put(name, value);
                break;
            case INTEGER:
                parent.put(name, value != null ? Long.parseLong(value) : 0L);
                break;
            case DOUBLE:
                parent.put(name, value != null ? Double.parseDouble(value) : 0.0);
                break;
            case BOOLEAN:
                parent.put(name, value != null ? Boolean.parseBoolean(value) : false);
                break;
            case OBJECT:
                ObjectNode objectNode = parent.putObject(name);
                for (FieldConfig child : field.getChildren()) {
                    buildField(child, objectNode, pathParams);
                }
                break;
            case ARRAY:
                ArrayNode arrayNode = parent.putArray(name);
                // 优先使用 children 构建数组
                if (field.getChildren() != null && !field.getChildren().isEmpty()) {
                    // Join children values and parse as JSON array
                    StringBuilder combined = new StringBuilder("[");
                    java.util.List<FieldConfig> childList = new java.util.ArrayList<>(field.getChildren());
                    for (int i = 0; i < childList.size(); i++) {
                        FieldConfig child = childList.get(i);
                        String childValue = resolveValue(child.getFieldValue(), pathParams);
                        if (childValue != null && !childValue.isEmpty()) {
                            String trimmed = childValue.trim();
                            if (trimmed.startsWith("{")) {
                                // It's a JSON object string - remove surrounding quotes and unescape
                                if (trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
                                    trimmed = trimmed.substring(1, trimmed.length() - 1);
                                    trimmed = trimmed.replace("\\\"", "\"").replace("\\\\", "\\");
                                }
                                combined.append(trimmed);
                            } else {
                                // Primitive value - quote it
                                combined.append("\"").append(childValue.replace("\"", "\\\"")).append("\"");
                            }
                        }
                        if (i < childList.size() - 1) {
                            combined.append(",");
                        }
                    }
                    combined.append("]");
                    try {
                        arrayNode.addAll((ArrayNode) objectMapper.readTree(combined.toString()));
                    } catch (Exception e) {
                        // Fallback: add as strings
                        for (FieldConfig child : childList) {
                            String childValue = resolveValue(child.getFieldValue(), pathParams);
                            arrayNode.add(childValue != null ? childValue : "");
                        }
                    }
                } else if (value != null && !value.isEmpty()) {
                    for (String item : value.split(",")) {
                        addArrayItem(arrayNode, item.trim());
                    }
                }
                break;
        }
    }

    private String resolveValue(String value, Map<String, String> pathParams) {
        if (value == null || pathParams == null) {
            return value;
        }

        Matcher matcher = PATH_PARAM_PATTERN.matcher(value);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String paramName = matcher.group(1);
            String paramValue = pathParams.get(paramName);
            matcher.appendReplacement(result, paramValue != null ? paramValue : "");
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private void addArrayItem(ArrayNode arrayNode, String value) {
        if (value.matches("-?\\d+")) {
            arrayNode.add(Long.parseLong(value));
        } else if (value.matches("-?\\d+\\.\\d+")) {
            arrayNode.add(Double.parseDouble(value));
        } else if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
            arrayNode.add(Boolean.parseBoolean(value));
        } else {
            arrayNode.add(value);
        }
    }

    public String matchPath(String configuredPath, String requestPath) {
        if (configuredPath.equals(requestPath)) {
            return configuredPath;
        }

        String regex = configuredPath.replaceAll("\\{[^}]+\\}", "([^/]+)");
        Pattern pattern = Pattern.compile("^" + regex + "$");
        Matcher matcher = pattern.matcher(requestPath);

        if (matcher.matches()) {
            return configuredPath;
        }
        return null;
    }

    public Map<String, String> extractPathParams(String configuredPath, String requestPath) {
        // Extract param names from the pattern
        List<String> paramNames = new ArrayList<>();
        Matcher nameMatcher = PATH_PARAM_PATTERN.matcher(configuredPath);
        while (nameMatcher.find()) {
            paramNames.add(nameMatcher.group(1));
        }

        if (paramNames.isEmpty()) {
            return null;
        }

        // Build regex with numbered groups
        String regex = configuredPath.replaceAll("\\{[^}]+\\}", "([^/]+)");
        Pattern pattern = Pattern.compile("^" + regex + "$");
        Matcher matcher = pattern.matcher(requestPath);

        if (matcher.matches()) {
            Map<String, String> result = new java.util.HashMap<>();
            for (int i = 0; i < paramNames.size(); i++) {
                result.put(paramNames.get(i), matcher.group(i + 1));
            }
            return result;
        }
        return null;
    }
}
