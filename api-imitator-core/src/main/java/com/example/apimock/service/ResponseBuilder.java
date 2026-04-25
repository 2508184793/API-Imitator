package com.example.apimock.service;

import com.example.apimock.entity.ApiConfig;
import com.example.apimock.entity.FieldConfig;
import com.example.apimock.entity.FieldType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ResponseBuilder {

    private static final Logger log = LoggerFactory.getLogger(ResponseBuilder.class);
    private final ObjectMapper objectMapper;

    // ========== v2 性能优化：正则表达式缓存 ==========
    private static final Pattern PATH_PARAM_PATTERN = Pattern.compile("\\{([^}]+)\\}");
    private final ConcurrentHashMap<String, Pattern> pathPatternCache = new ConcurrentHashMap<>();

    // 注入 Spring 管理的 ObjectMapper 单例
    public ResponseBuilder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

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
            log.error("构建响应失败", e);
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
                try {
                    parent.put(name, value != null ? Long.parseLong(value) : 0L);
                } catch (NumberFormatException e) {
                    log.warn("整数字段解析失败: {} = {}, 使用默认值 0", name, value);
                    parent.put(name, 0L);
                }
                break;
            case DOUBLE:
                try {
                    parent.put(name, value != null ? Double.parseDouble(value) : 0.0);
                } catch (NumberFormatException e) {
                    log.warn("浮点数字段解析失败: {} = {}, 使用默认值 0.0", name, value);
                    parent.put(name, 0.0);
                }
                break;
            case BOOLEAN:
                parent.put(name, value != null ? Boolean.parseBoolean(value) : false);
                break;
            case OBJECT:
                ObjectNode objectNode = parent.putObject(name);
                log.info("对象字段 {} 有 {} 个子项", name, field.getChildren() != null ? field.getChildren().size() : 0);
                for (FieldConfig child : field.getChildren()) {
                    buildField(child, objectNode, pathParams);
                }
                break;
            case ARRAY:
                ArrayNode arrayNode = parent.putArray(name);
                // 优先使用 children 构建数组
                if (field.getChildren() != null && !field.getChildren().isEmpty()) {
                    log.info("数组字段 {} 有 {} 个子项", name, field.getChildren().size());
                    for (FieldConfig child : field.getChildren()) {
                        // 如果是 OBJECT 类型且有 children，递归构建对象
                        if (child.getFieldType() == FieldType.OBJECT && 
                            child.getChildren() != null && !child.getChildren().isEmpty()) {
                            log.info("  子项是 OBJECT 类型，有 {} 个子属性", child.getChildren().size());
                            ObjectNode objNode = arrayNode.addObject();
                            for (FieldConfig prop : child.getChildren()) {
                                buildField(prop, objNode, pathParams);
                            }
                        } else {
                            // 其他类型用 value
                            String childValue = resolveValue(child.getFieldValue(), pathParams);
                            log.info("  子项 {} 值: {}", child.getFieldName(), childValue);
                            if (childValue != null && !childValue.isEmpty()) {
                                String trimmed = childValue.trim();
                                try {
                                    JsonNode node = objectMapper.readTree(trimmed);
                                    arrayNode.add(node);
                                } catch (Exception e) {
                                    arrayNode.add(childValue);
                                }
                            }
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

    /**
     * v2 优化：使用缓存的正则表达式进行路径匹配
     */
    public String matchPath(String configuredPath, String requestPath) {
        if (configuredPath.equals(requestPath)) {
            return configuredPath;
        }

        Pattern pattern = pathPatternCache.computeIfAbsent(configuredPath, k -> {
            String regex = k.replaceAll("\\{[^}]+\\}", "([^/]+)");
            return Pattern.compile("^" + regex + "$");
        });

        Matcher matcher = pattern.matcher(requestPath);
        if (matcher.matches()) {
            return configuredPath;
        }
        return null;
    }

    /**
     * v2 优化：复用 matchPath 的匹配结果，避免二次正则匹配
     */
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

        // 使用缓存的正则
        Pattern pattern = pathPatternCache.computeIfAbsent(configuredPath, k -> {
            String regex = k.replaceAll("\\{[^}]+\\}", "([^/]+)");
            return Pattern.compile("^" + regex + "$");
        });

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

    /**
     * v2 新增：解析响应头（JSON 字符串 -> Map）
     */
    public Map<String, String> parseResponseHeaders(String headersJson) {
        if (headersJson == null || headersJson.isEmpty()) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(headersJson, Map.class);
        } catch (Exception e) {
            log.warn("解析响应头失败: {}", headersJson, e);
            return Collections.emptyMap();
        }
    }

    /**
     * v2 新增：序列化响应头（Map -> JSON 字符串）
     */
    public String serializeResponseHeaders(Map<String, String> headers) {
        if (headers == null || headers.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(headers);
        } catch (Exception e) {
            log.warn("序列化响应头失败", e);
            return null;
        }
    }

    /**
     * 清除缓存（配置变更时调用）
     */
    public void clearCache() {
        pathPatternCache.clear();
        log.info("路径匹配缓存已清除，当前缓存大小: {}", pathPatternCache.size());
    }
}
