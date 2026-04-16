# syntax=docker/dockerfile:1
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace

COPY pom.xml .
COPY discovery-service/pom.xml discovery-service/
COPY config-service/pom.xml config-service/
COPY api-gateway/pom.xml api-gateway/
COPY user-service/pom.xml user-service/
COPY game-service/pom.xml game-service/
COPY tracking-service/pom.xml tracking-service/
COPY alert-service/pom.xml alert-service/
COPY ml-service/pom.xml ml-service/
COPY medical-service/pom.xml medical-service/
COPY medicament-validation-service/pom.xml medicament-validation-service/
COPY iot-service/pom.xml iot-service/
COPY assistant-service/pom.xml assistant-service/
COPY analytics-service/pom.xml analytics-service/

RUN --mount=type=cache,target=/root/.m2 mvn dependency:go-offline -B

COPY assistant-service/ assistant-service/
RUN --mount=type=cache,target=/root/.m2 mvn package -DskipTests -B -pl assistant-service

FROM eclipse-temurin:17-jre-jammy
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=build /workspace/assistant-service/target /tmp/target
RUN find /tmp/target -maxdepth 1 -name '*.jar' ! -name '*-plain.jar' | head -1 | xargs -I{} cp {} /app/app.jar && rm -rf /tmp/target
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0"
EXPOSE 18089
ENTRYPOINT ["java", "-jar", "app.jar"]
