FROM gradle:8-jdk17 AS build
WORKDIR /app
COPY build.gradle settings.gradle ./
COPY gradle ./gradle
COPY scripts ./scripts
RUN gradle dependencies --no-daemon
COPY src ./src
RUN gradle bootWar --no-daemon -x test

FROM eclipse-temurin:17-jre-alpine
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
WORKDIR /app
COPY --from=build /app/build/libs/app.war app.war
RUN chown appuser:appgroup app.war
USER appuser
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.war"]
