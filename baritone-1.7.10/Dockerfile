FROM openjdk:8-jdk-slim

WORKDIR /app

COPY . .

RUN ./gradlew setupDecompWorkspace && ./gradlew build

CMD ["./gradlew", "build"]