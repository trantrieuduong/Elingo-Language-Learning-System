# =============================================================================
# Stage 1: Build JAR package
# =============================================================================
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw
RUN ./mvnw dependency:go-offline -B

COPY src ./src
RUN ./mvnw clean package -DskipTests -B

# =============================================================================
# Stage 2: Runtime Image (Lightweight, Secure & Non-root)
# =============================================================================
FROM eclipse-temurin:21-jre-alpine AS runner

WORKDIR /app

# Tạo non-root user và group để tuân thủ tiêu chuẩn bảo mật container
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

COPY --from=builder --chown=appuser:appgroup /app/target/*.jar app.jar

USER appuser

EXPOSE 8080

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]