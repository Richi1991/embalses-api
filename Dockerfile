FROM maven:3.8.5-openjdk-17 AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests -Djooq.codegen.skip=true

FROM eclipse-temurin:17-jdk-jammy
COPY _.chsegura.es.crt /usr/local/share/ca-certificates/chs_root.crt
RUN apt-get update && apt-get install -y \
    wget gnupg unzip curl libnss3 libxss1 libasound2 \
    libatk1.0-0 libc6 ca-certificates fonts-liberation xdg-utils \
    --no-install-recommends \
    && update-ca-certificates \
    && wget -q -O - https://dl-ssl.google.com/linux/linux_signing_key.pub | apt-key add - \
    && sh -c 'echo "deb [arch=amd64] http://dl.google.com/linux/chrome/deb/ stable main" >> /etc/apt/sources.list.d/google.list' \
    && apt-get update \
    && apt-get install -y google-chrome-stable \
    && keytool -import -trustcacerts \
        -alias chs_cert \
        -file /usr/local/share/ca-certificates/chs_root.crt \
        -keystore $JAVA_HOME/lib/security/cacerts \
        -storepass changeit \
        -noprompt \
    && rm -rf /var/lib/apt/lists/*

ENV CHROME_BIN=/usr/bin/google-chrome-stable
COPY --from=build /app/target/embalseschs-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]