# Etapa de compilación
FROM maven:3.8.5-openjdk-17 AS build
COPY . .
RUN mvn clean package -DskipTests

# Etapa de ejecución
FROM eclipse-temurin:17-jdk-jammy

# Instalar dependencias y Google Chrome en una sola capa para reducir tamaño
RUN apt-get update && apt-get install -y \
    wget gnupg unzip curl libnss3 libxss1 libasound2 \
    libatk1.0-0 libc6 ca-certificates fonts-liberation xdg-utils \
    --no-install-recommends \
    && wget -q -O - https://dl-ssl.google.com/linux/linux_signing_key.pub | apt-key add - \
    && sh -c 'echo "deb [arch=amd64] http://dl.google.com/linux/chrome/deb/ stable main" >> /etc/apt/sources.list.d/google.list' \
    && apt-get update \
    && apt-get install -y google-chrome-stable \
    && rm -rf /var/lib/apt/lists/*

# Definir la ruta del binario para que Java lo encuentre
ENV CHROME_BIN=/usr/bin/google-chrome-stable

# Copiar el jar generado (Asegúrate de que el nombre coincide con tu pom.xml)
COPY --from=build /target/embalseschs-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]