# Build stage
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn package -DskipTests

# Runtime stage
FROM eclipse-temurin:17-jre
WORKDIR /app

# Create a dedicated non-root user
RUN groupadd --gid 10001 apprunner && \
    useradd --uid 10001 --gid 10001 \
    --no-create-home --shell /usr/sbin/nologin apprunner

# Copy the application
COPY --from=build --chown=10001:10001 /app/target/*.jar app.jar

# Run as a non-root user
USER 10001:10001
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]