# Etapa de compilación
FROM maven:3.8.5-openjdk-17 AS build
COPY . .


# --- NUEVA SECCIÓN PARA JOOQ ---
# Creamos el archivo que Maven busca usando variables de entorno de Render
RUN echo "db.url=${SPRING_DATASOURCE_URL}" > jooq-codegen.properties && \
    echo "db.user=${SPRING_DATASOURCE_USERNAME}" >> jooq-codegen.properties && \
    echo "db.password=${SPRING_DATASOURCE_PASSWORD}" >> jooq-codegen.properties
# -------------------------------

RUN mvn clean package -DskipTests

# Etapa de ejecución
FROM eclipse-temurin:17-jdk-jammy

# 1. Copiar el certificado de la CHS al contenedor
# El archivo debe estar en la raíz de tu proyecto (junto al Dockerfile)
COPY _.chsegura.es.crt /usr/local/share/ca-certificates/chs_root.crt

# Instalar dependencias y Google Chrome
RUN apt-get update && apt-get install -y \
    wget gnupg unzip curl libnss3 libxss1 libasound2 \
    libatk1.0-0 libc6 ca-certificates fonts-liberation xdg-utils \
    --no-install-recommends \
    # Actualizar certificados del sistema operativo
    && update-ca-certificates \
    # Instalar Google Chrome
    && wget -q -O - https://dl-ssl.google.com/linux/linux_signing_key.pub | apt-key add - \
    && sh -c 'echo "deb [arch=amd64] http://dl.google.com/linux/chrome/deb/ stable main" >> /etc/apt/sources.list.d/google.list' \
    && apt-get update \
    && apt-get install -y google-chrome-stable \
    # 2. Importar el certificado en el TrustStore de Java (cacerts)
    && keytool -import -trustcacerts \
        -alias chs_cert \
        -file /usr/local/share/ca-certificates/chs_root.crt \
        -keystore $JAVA_HOME/lib/security/cacerts \
        -storepass changeit \
        -noprompt \
    && rm -rf /var/lib/apt/lists/*

# Definir la ruta del binario para que Java lo encuentre
ENV CHROME_BIN=/usr/bin/google-chrome-stable

# Copiar el jar generado
COPY --from=build /target/embalseschs-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]