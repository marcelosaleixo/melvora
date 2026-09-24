# ================================
# MELVORA - EasyPanel / Production
# Java 21 + Spring Boot + PostgreSQL + Flyway
# ================================

FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

# Cache de dependências Maven
COPY pom.xml .
RUN mvn -B -DskipTests dependency:go-offline

# Código-fonte
COPY src ./src

# Build limpo para produção
RUN mvn -B clean package -DskipTests

# ================================
# Runtime
# ================================
FROM eclipse-temurin:21-jre

WORKDIR /app

ENV TZ=America/Cuiaba
ENV SERVER_PORT=8080
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"

COPY --from=build /app/target/*.jar /app/melvora.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/melvora.jar"]
