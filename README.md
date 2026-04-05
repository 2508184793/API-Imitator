# API-Imitator

一个基于 Spring Boot 的 API Mock 服务器，允许通过配置文件或 Web UI 动态配置 API 返回数据。

## 功能特性

- **动态配置 API**：无需编码即可创建新的 API 端点
- **支持多种数据类型**：STRING、INTEGER、DOUBLE、BOOLEAN、OBJECT、ARRAY
- **嵌套对象**：支持多层级嵌套的 JSON 结构
- **路径参数**：支持 `/api/test/{id}` 形式的动态路径
- **参数引用**：可在返回值中引用路径参数，如 `{id}`
- **Web UI**：提供可视化界面管理 API 配置
- **热生效**：配置保存后立即生效

## 技术栈

- Spring Boot 3.2
- MySQL（可切换至其他关系型数据库）
- JPA / Hibernate
- YAML 配置
- 原生 HTML/CSS/JS（无前端框架）

## 快速开始

### 1. 启动服务

```bash
mvn spring-boot:run
```

服务启动后访问：http://localhost:8080/

### 2. 创建 API 配置

通过 Web UI 或 REST API 创建配置：

```bash
curl -X POST http://localhost:8080/api/configs \
  -H "Content-Type: application/json" \
  -d '{
    "path": "/api/test/{id}",
    "method": "GET",
    "description": "测试接口",
    "fields": [
      {"name": "string", "type": "OBJECT", "children": [
        {"name": "a", "type": "STRING", "value": "1"}
      ]},
      {"name": "int", "type": "OBJECT", "children": [
        {"name": "b", "type": "INTEGER", "value": "2"}
      ]}
    ]
  }'
```

### 3. 访问动态 API

```bash
curl http://localhost:8080/api/test/123
# 返回: {"string":{"a":"1"},"int":{"b":2}}
```

## API 接口

### 配置管理 API

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/configs | 获取所有配置 |
| GET | /api/configs/{id} | 获取单个配置 |
| POST | /api/configs | 创建配置 |
| PUT | /api/configs/{id} | 更新配置 |
| DELETE | /api/configs/{id} | 删除配置 |

### 动态 API

| 方法 | 路径 | 说明 |
|------|------|------|
| * | /dynamic/** | 根据配置动态响应 |

## 字段类型说明

| 类型 | 说明 | 示例值 |
|------|------|--------|
| STRING | 字符串 | "hello" |
| INTEGER | 整数 | 123 |
| DOUBLE | 小数 | 1.23 |
| BOOLEAN | 布尔值 | true/false |
| OBJECT | 嵌套对象 | {"name": "value"} |
| ARRAY | 数组 | 逗号分隔值或嵌套对象数组 |

## 配置示例

### 基础用法

```json
{
  "path": "/api/user",
  "method": "GET",
  "fields": [
    {"name": "name", "type": "STRING", "value": "张三"},
    {"name": "age", "type": "INTEGER", "value": "25"}
  ]
}
```

响应：`{"name": "张三", "age": 25}`

### 使用路径参数

```json
{
  "path": "/api/user/{id}",
  "method": "GET",
  "fields": [
    {"name": "id", "type": "STRING", "value": "{id}"},
    {"name": "name", "type": "STRING", "value": "张三"}
  ]
}
```

请求：`GET /api/user/100`
响应：`{"id": "100", "name": "张三"}`

### 嵌套对象

```json
{
  "path": "/api/data",
  "method": "GET",
  "fields": [
    {"name": "user", "type": "OBJECT", "children": [
      {"name": "name", "type": "STRING", "value": "张三"},
      {"name": "age", "type": "INTEGER", "value": "25"}
    ]}
  ]
}
```

响应：`{"user": {"name": "张三", "age": 25}}`

### 数组类型

```json
{
  "path": "/api/list",
  "method": "GET",
  "fields": [
    {"name": "ids", "type": "ARRAY", "value": "1,2,3,4,5"}
  ]
}
```

响应：`{"ids": [1, 2, 3, 4, 5]}`

### 数组类型（嵌套对象）

ARRAY 类型的子节点会作为 JSON 数组中的对象输出：

```json
{
  "path": "/api/users",
  "method": "GET",
  "fields": [
    {"name": "users", "type": "ARRAY", "children": [
      {"name": "name", "type": "STRING", "value": "张三"},
      {"name": "age", "type": "INTEGER", "value": "25"}
    ]},
    {"name": "users", "type": "ARRAY", "children": [
      {"name": "name", "type": "STRING", "value": "李四"},
      {"name": "age", "type": "INTEGER", "value": "30"}
    ]}
  ]
}
```

响应：`{"users": [{"name": "张三", "age": 25}, {"name": "李四", "age": 30}]}`

## 数据库切换

当前使用 MySQL，配置文件为 `application.yml`。如需切换到其他数据库：

1. 修改 `pom.xml` 添加对应驱动依赖
2. 更新 `application.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/api_mock?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: root
    password: your_password
    driver-class-name: com.mysql.cj.jdbc.Driver
  jpa:
    database-platform: org.hibernate.dialect.MySQLDialect
    hibernate:
      ddl-auto: update
```

## 项目结构

```
src/main/java/com/example/apimock/
├── ApiMockApplication.java     # 启动类
├── config/
│   └── WebConfig.java         # Web 配置
├── controller/
│   ├── ApiConfigController.java    # 配置管理 API
│   └── DynamicApiController.java   # 动态响应处理
├── dto/
│   ├── ApiConfigRequest.java
│   ├── ApiConfigResponse.java
│   └── FieldConfigDto.java
├── entity/
│   ├── ApiConfig.java         # API 配置实体
│   ├── FieldConfig.java       # 字段配置实体
│   └── FieldType.java         # 字段类型枚举
├── repository/
│   ├── ApiConfigRepository.java
│   └── FieldConfigRepository.java
└── service/
    ├── ApiConfigService.java   # 配置管理服务
    └── ResponseBuilder.java   # 响应构建器
```
