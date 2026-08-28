# syntax=docker/dockerfile:1
FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace
COPY mvnw pom.xml ./
COPY .mvn .mvn
COPY game-engine/pom.xml game-engine/pom.xml
COPY game-session/pom.xml game-session/pom.xml
RUN --mount=type=cache,target=/root/.m2 ./mvnw -q -pl game-session -am dependency:go-offline
COPY game-session/src game-session/src
RUN --mount=type=cache,target=/root/.m2 ./mvnw -q -pl game-session -am -DskipTests package \
    && cp game-session/target/*.jar /workspace/app.jar

FROM eclipse-temurin:25-jre
# curl is only needed by the container healthcheck
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --system --uid 1001 app
USER app
WORKDIR /app
COPY --from=build /workspace/app.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-XX:+UseZGC", "-Duser.timezone=UTC", "-jar", "app.jar"]
