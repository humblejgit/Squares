# syntax=docker/dockerfile:1
FROM maven:3.9.11-eclipse-temurin-21-alpine AS builder

WORKDIR /workspace
COPY . .
RUN --mount=type=cache,target=/root/.m2/repository \
    mvn -B -pl squares-server -am -DskipTests package

FROM eclipse-temurin:21-jre-alpine

WORKDIR /application
RUN addgroup -S squares && adduser -S squares -G squares
COPY --from=builder --chown=squares:squares /workspace/squares-server/target/squares-server-*.jar application.jar

USER squares
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "application.jar"]
