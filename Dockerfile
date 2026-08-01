# syntax=docker/dockerfile:1
# Multi-stage build producing a Cloud Run-compatible container (PRD-NFR-002).

FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
# Copy the whole reactor and build only the app module plus its dependencies.
COPY . .
RUN mvn -q -B -pl app -am -DskipTests clean package

FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app
# Run as a non-root user.
RUN groupadd -r allocra && useradd -r -g allocra -u 1001 allocra
COPY --from=build /workspace/app/target/app-*.jar /app/app.jar
USER 1001
# Cloud Run injects PORT; server.port binds to it (see application.yml). Default profile
# for a deployed container is 'cloud' (structured logging).
ENV SPRING_PROFILES_ACTIVE=cloud
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
