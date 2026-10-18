FROM eclipse-temurin:21-jdk AS build

WORKDIR /workspace
COPY build.gradle settings.gradle gradle.properties gradlew ./
COPY gradle ./gradle
COPY src ./src

RUN chmod +x gradlew && ./gradlew --no-daemon clean bootJar

FROM eclipse-temurin:21-jre

WORKDIR /app
RUN apt-get update && apt-get install -y --no-install-recommends wget tar && rm -rf /var/lib/apt/lists/*
RUN groupadd --system app && useradd --system --gid app --home-dir /app --no-create-home app
RUN mkdir -p /app/data/hikvision && chown -R app:app /app
COPY --from=build --chown=app:app /workspace/build/libs/*.jar app.jar

USER app
EXPOSE 8080 8081

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
