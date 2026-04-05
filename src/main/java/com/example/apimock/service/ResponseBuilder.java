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
                parent.put(name, value != null ? Integer.parseInt(value) : 0);
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
                    for (FieldConfig child : field.getChildren()) {
                        String childValue = resolveValue(child.getFieldValue(), pathParams);
                        switch (child.getFieldType()) {
                            case INTEGER:
                                arrayNode.add(childValue != null && !childValue.isEmpty() ? Integer.parseInt(childValue) : 0);
                                break;
                            case DOUBLE:
                                arrayNode.add(childValue != null && !childValue.isEmpty() ? Double.parseDouble(childValue) : 0.0);
                                break;
                            case BOOLEAN:
                                arrayNode.add(childValue != null && !childValue.isEmpty() ? Boolean.parseBoolean(childValue) : false);
                                break;
                            default:
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
            arrayNode.add(Integer.parseInt(value));
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
