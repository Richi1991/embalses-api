# PASO 1: Compilación (Build)
FROM maven:3.8.5-openjdk-17 AS build
COPY . .
RUN mvn clean package -DskipTests

# PASO 2: Ejecución (Run)
FROM openjdk:17.0.1-jdk-slim
WORKDIR /app
COPY --from=build target/embalseschs-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
