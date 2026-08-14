FROM gradle:8.14.4-jdk21 AS build

WORKDIR /app

COPY . .
RUN chmod +x gradlew && ./gradlew bootJar --no-daemon

FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8000

ENV SERVER_PORT=8000

ENTRYPOINT ["java", "-jar", "/app/app.jar"]