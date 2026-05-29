# ============================================
# Stage 1: Build with Maven + JDK 21
# ============================================
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# 优先复制 pom.xml，利用 Docker layer 缓存依赖
COPY pom.xml .
RUN mvn dependency:go-offline -B

# 复制源码并打包
COPY src ./src
RUN mvn package -DskipTests -B

# ============================================
# Stage 2: Runtime with JRE 21 only
# ============================================
FROM eclipse-temurin:21-jre
WORKDIR /app

# 从构建阶段复制 fat JAR
COPY --from=build /build/target/*.jar app.jar

EXPOSE 8180

# JAVA_OPTS 由 docker-compose override 文件按挡位设置
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
