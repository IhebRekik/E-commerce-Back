# Stage 1: Build the JAR using Maven
FROM maven:3.9.8-eclipse-temurin-21 AS build

WORKDIR /app
COPY . .

RUN mvn clean install -DskipTests

# Stage 2: Run the JAR using OpenJDK 21
FROM openjdk:21-jdk

WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
