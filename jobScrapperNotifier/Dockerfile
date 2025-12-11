# ===========================
# Build Stage
# ===========================
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src src/
RUN mvn clean package -DskipTests

# ===========================
# Run Stage with Chrome + Chromedriver
# ===========================
FROM selenium/standalone-chrome:latest

USER root
WORKDIR /app

# Copy the Spring Boot JAR
COPY --from=build /app/target/*.jar app.jar

# Expose Spring Boot port
EXPOSE 8080

# Run the app
ENTRYPOINT ["java", "-jar", "app.jar"]
