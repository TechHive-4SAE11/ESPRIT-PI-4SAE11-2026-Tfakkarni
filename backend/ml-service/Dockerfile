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

RUN mvn dependency:go-offline -B

COPY ml-service/ ml-service/
RUN mvn package -DskipTests -B -pl ml-service

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=build /workspace/ml-service/target/*.jar app.jar
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0"
EXPOSE 18086
ENTRYPOINT ["java", "-jar", "app.jar"]
