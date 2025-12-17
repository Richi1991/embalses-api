# PASO 1: Compilación (Build)
# Usamos una imagen de Maven con Java 17 para construir el proyecto
FROM maven:3.8.5-openjdk-17 AS build
COPY . .
# Compilamos el proyecto omitiendo los tests para acelerar el despliegue
RUN mvn clean package -DskipTests

# PASO 2: Ejecución (Run)
# Usamos una imagen de Java más ligera para ejecutar la app
FROM openjdk:17.0.1-jdk-slim
# Copiamos el archivo .jar generado en el paso anterior
# Asegúrate de que el nombre coincida con el <finalName> de tu pom.xml
COPY --from=build /target/*.jar app.jar

# Exponemos el puerto que usa Spring Boot por defecto
EXPOSE 8080

# Comando para arrancar la aplicación
ENTRYPOINT ["java", "-jar", "app.jar"]