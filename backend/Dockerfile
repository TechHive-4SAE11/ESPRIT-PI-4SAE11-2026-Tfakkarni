# syntax=docker/dockerfile:1
# Build one Spring Boot module: docker build --build-arg MODULE=medical-service -t tfakkarni/medical-service .
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY . .
ARG MODULE=discovery-service
RUN --mount=type=cache,target=/root/.m2 mvn -B -DskipTests package -pl "${MODULE}"

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
ARG MODULE=discovery-service
RUN apt-get update && DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends findutils && rm -rf /var/lib/apt/lists/*
COPY --from=build /workspace/${MODULE}/target /workspace-target
RUN JAR="$(find /workspace-target -maxdepth 1 -name '*.jar' ! -name '*-plain.jar' | head -1)" && \
    test -n "$JAR" && cp "$JAR" /app/app.jar && rm -rf /workspace-target
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0"
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
