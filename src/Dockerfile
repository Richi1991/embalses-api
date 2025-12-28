FROM eclipse-temurin:17-jdk-jammy

# Instalar Chromium y dependencias
RUN apt-get update && apt-get install -y \
    chromium \
    chromium-driver \
    fonts-liberation \
    libatk-bridge2.0-0 \
    libatk1.0-0 \
    libcups2 \
    libdrm2 \
    libxkbcommon0 \
    libxcomposite1 \
    libxrandr2 \
    libgbm1 \
    libasound2 \
    && rm -rf /var/lib/apt/lists/*

# Variable de entorno (opcional pero útil)
ENV CHROME_BIN=/usr/bin/chromium

WORKDIR /app

# Copiar el jar
COPY target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java","-jar","/app/app.jar"]
