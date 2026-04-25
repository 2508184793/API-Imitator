# ==========================================
# API-Imitator v2 Dockerfile
# ==========================================
# 多阶段构建：构建 + 运行分离，镜像更小更安全

# ---------- 构建阶段 ----------
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /app

# 先复制 pom 文件，利用 Docker 缓存
COPY pom.xml .
COPY api-imitator-core/pom.xml ./api-imitator-core/
COPY api-imitator-app/pom.xml ./api-imitator-app/

# 下载依赖（缓存层）
RUN mvn dependency:go-offline -B

# 复制源代码
COPY api-imitator-core/src ./api-imitator-core/src
COPY api-imitator-app/src ./api-imitator-app/src

# 构建项目
RUN mvn clean package -DskipTests -B

# ---------- 运行阶段 ----------
FROM eclipse-temurin:17-jre

WORKDIR /app

# 从构建阶段复制 jar 文件
COPY --from=builder /app/api-imitator-app/target/*.jar app.jar

# 暴露端口
EXPOSE 8080

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/ || exit 1

# 启动命令
ENTRYPOINT ["java", "-Xmx512m", "-Xms256m", "-jar", "app.jar"]