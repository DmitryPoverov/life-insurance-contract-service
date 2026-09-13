FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /build

COPY gradlew ./
COPY gradle ./gradle
COPY settings.gradle.kts ./
COPY insurance-service/build.gradle.kts ./insurance-service/
COPY registry-emulator/build.gradle.kts ./registry-emulator/

RUN chmod +x gradlew \
    && ./gradlew --no-daemon :registry-emulator:dependencies \
        --configuration runtimeClasspath > /dev/null

COPY registry-emulator/src ./registry-emulator/src
RUN ./gradlew --no-daemon :registry-emulator:bootJar

RUN java -Djarmode=tools -jar registry-emulator/build/libs/application.jar \
    extract --layers --destination /extracted

FROM eclipse-temurin:21-jre-alpine
WORKDIR /application

RUN addgroup -S app && adduser -S -G app app

COPY --from=build --chown=app:app /extracted/dependencies/ ./
COPY --from=build --chown=app:app /extracted/spring-boot-loader/ ./
COPY --from=build --chown=app:app /extracted/snapshot-dependencies/ ./
COPY --from=build --chown=app:app /extracted/application/ ./

USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "application.jar"]
