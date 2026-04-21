FROM eclipse-temurin:25-jdk AS build
WORKDIR /app
COPY gradlew ./
COPY gradle ./gradle
COPY build.gradle settings.gradle ./
COPY scripts ./scripts
RUN ./gradlew dependencies --no-daemon
COPY src ./src
RUN ./gradlew bootWar --no-daemon -x test

FROM openjdk:25-slim
RUN addgroup -S appgroup && adduser -S appuser --ingroup appgroup
WORKDIR /app
COPY --from=build /app/build/libs/app.war app.war
RUN chown appuser:appgroup app.war
USER appuser
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.war"]
