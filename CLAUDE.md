# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is an API Mock Server built with Spring Boot. It allows dynamic configuration of API endpoints and their response data through a web UI or REST API.

## Build & Run

```bash
# Build
mvn clean package

# Run
mvn spring-boot:run

# Or run the JAR directly
java -jar target/api-imitator-1.0-SNAPSHOT.jar
```

## Architecture

The application has two main request handling paths:

1. **Management API** (`/api/configs/*`) - CRUD operations for API configurations via `ApiConfigController`
2. **Dynamic API** (`/dynamic/**`) - Catches all other requests and returns configured responses via `DynamicApiController`

### Key Components

- `ResponseBuilder` - Converts `FieldConfig` entities into JSON responses, supports nested objects and path parameter substitution (e.g., `{id}` in field value resolves to actual path parameter)
- `ApiConfigService` - Manages API configuration persistence with nested field hierarchy
- `DynamicApiController` - Uses `/**` catch-all pattern; matches incoming requests against stored configurations by comparing paths (with `{param}` pattern matching) and HTTP methods

### Database

- SQLite by default (`api-imitator.db`), configured in `application.properties`
- Switch to other relational databases by updating `spring.datasource.url`, `spring.datasource.driver-class-name`, and `spring.jpa.database-platform`
- `spring.jpa.hibernate.ddl-auto=update` auto-creates tables

### Field Types

`FieldType` enum: `STRING`, `INTEGER`, `DOUBLE`, `BOOLEAN`, `OBJECT`, `ARRAY`

- `OBJECT` type supports recursive nested children
- `ARRAY` values are comma-separated strings
- Field values support path parameter references like `{paramName}` which get substituted at response time

### Frontend

Single-page UI at `src/main/resources/static/index.html` - provides interface for creating/editing API configurations and testing endpoints.
