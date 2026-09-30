# Build consistente com o runtime (Java 21)
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -B -DskipTests clean package

FROM eclipse-temurin:21-jre
WORKDIR /app
ENV TZ=America/Cuiaba
ENV SERVER_PORT=8086
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"
COPY --from=build /app/target/melvora-*.jar /app/melvora.jar
EXPOSE 8086
ENTRYPOINT ["java", "-jar", "/app/melvora.jar"]
