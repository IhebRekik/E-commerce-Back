# Stage 1: Build the JAR using Maven
FROM maven:3.9.2-eclipse-temurin-21 AS build

# Set working directory inside container
WORKDIR /app

# Copy Maven configuration first for caching
COPY pom.xml .

# Copy source code
COPY src ./src

# Build the Spring Boot JAR (skip tests to speed up)
RUN mvn clean package -DskipTests

# Stage 2: Create lightweight runtime image
FROM eclipse-temurin:21-jdk

WORKDIR /app

# Copy the JAR from the build stage
COPY --from=build /app/target/*.jar app.jar

# Run the JAR
ENTRYPOINT ["java","-jar","/app.jar"]
