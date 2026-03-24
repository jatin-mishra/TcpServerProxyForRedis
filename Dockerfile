FROM eclipse-temurin:17-jdk AS builder
WORKDIR /app

COPY gradlew .
COPY gradle/ gradle/
COPY build.gradle .
COPY settings.gradle .
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon


COPY src/ src/
RUN ./gradlew shadowJar --no-daemon

FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=builder /app/build/libs/BrowserStackMachineCodding-1.0-SNAPSHOT.jar app.jar
EXPOSE 9736
ENTRYPOINT ["java", "-jar", "app.jar"]