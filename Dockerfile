# Use Temurin JDK 21
FROM eclipse-temurin:21-jdk

# Define JAR file path (built by Maven)
ARG JAR_FILE=target/*.jar

# Copy the JAR into the image
COPY ${JAR_FILE} app.jar

# Run the JAR
ENTRYPOINT ["java","-jar","/app.jar"]
