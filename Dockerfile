# syntax=docker/dockerfile:1
# -----------------------------------------------------------------------------
# Multi-stage Dockerfile para investidor-inteligente
#
# NOTA: Este Dockerfile e OPCIONAL. O projeto foi pensado para rodar "sem Docker"
# com PostgreSQL embutido no proprio processo Spring (vide DatabaseConfig.java).
# O Dockerfile esta aqui para:
#   1. Demonstrar conhecimento de containerizacao (multi-stage, JRE slim, cache)
#   2. Permitir rodar em ambientes onde o usuario prefere isolamento via container
#
# Ao rodar containerizado, Postgres continua embutido no mesmo container.
# Se quiser quebrar em servicos separados, veja docker-compose.yml com os
# comentarios explicando as opcoes.
# -----------------------------------------------------------------------------

# ---- Stage 1: build ----
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /workspace

# Copia primeiro apenas o pom para aproveitar cache de dependencias
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .
RUN ./mvnw --batch-mode --no-transfer-progress dependency:go-offline

# Agora copia o codigo e constroi o jar
COPY src src
RUN ./mvnw --batch-mode --no-transfer-progress package -DskipTests \
    && mv target/investidor-inteligente-*.jar target/app.jar

# ---- Stage 2: runtime ----
FROM eclipse-temurin:17-jre-alpine

# Usuario nao-root para seguranca
RUN addgroup -S app && adduser -S app -G app

WORKDIR /app

COPY --from=build /workspace/target/app.jar app.jar

EXPOSE 8084

USER app

ENTRYPOINT ["java", "-jar", "app.jar"]
