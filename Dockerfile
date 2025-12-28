# 1️⃣ Imagen base con JDK 17
FROM eclipse-temurin:17-jdk-alpine

# 2️⃣ Instalar dependencias para Chromium y Selenium
RUN apk add --no-cache \
    chromium \
    chromium-chromedriver \
    bash \
    curl \
    ttf-freefont \
    nss \
    freetype \
    harfbuzz \
    ca-certificates \
    fontconfig \
    && update-ca-certificates

# 3️⃣ Variables de entorno para Selenium
ENV CHROME_BIN=/usr/bin/chromium-browser
ENV PATH=$PATH:/usr/bin

# 4️⃣ Crear directorio de la app
WORKDIR /app

# 5️⃣ Copiar pom.xml y código fuente
COPY pom.xml .
COPY src ./src
COPY mvnw .
COPY .mvn .mvn

# 6️⃣ Construir el proyecto con Maven
RUN chmod +x mvnw && ./mvnw clean package -DskipTests

# 7️⃣ Exponer puerto
ENV PORT=8080
EXPOSE 8080

# 8️⃣ Comando para arrancar la app
ENTRYPOINT ["java", "-jar", "target/embalseschs-0.0.1-SNAPSHOT.jar"]
