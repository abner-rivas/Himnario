FROM gradle:9.6.0-jdk21-alpine AS build

WORKDIR /workspace

COPY settings.gradle.kts build.gradle.kts gradle.properties ./
RUN gradle --no-daemon --quiet dependencies --configuration runtimeClasspath > /dev/null

COPY src src
RUN gradle --no-daemon clean buildFatJar -x test

FROM eclipse-temurin:21-jre-alpine AS runtime

RUN addgroup -S himnario && adduser -S himnario -G himnario
WORKDIR /app

COPY --from=build --chown=himnario:himnario /workspace/build/libs/himnario-api-all.jar /app/himnario-api.jar

USER himnario
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/himnario-api.jar"]
