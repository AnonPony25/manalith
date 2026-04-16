# ── Build stage ──────────────────────────────────────────────────────
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /workspace

COPY . .
RUN ./mvnw clean package -pl manalith-api -am -DskipTests

# ── Runtime stage ─────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine AS runtime
WORKDIR /app

COPY --from=build /workspace/manalith-api/target/*.jar app.jar

RUN addgroup -S manalith && adduser -S manalith -G manalith
USER manalith

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
