# Multi-stage build for a lightweight Ktor server Docker image

# Stage 1: Cache Gradle dependencies
FROM gradle:8.10.2-jdk17-alpine AS cache
WORKDIR /home/gradle/cache
ENV GRADLE_USER_HOME /home/gradle/cache_home

# Copy only the necessary files for dependency resolution
COPY gradle/ gradle/
COPY gradle.properties settings.gradle.kts build.gradle.kts ./
COPY gradlew ./
COPY gradlew.bat ./

# Create empty directories for required modules to satisfy dependency resolution
RUN mkdir -p server

# Download dependencies only (faster than full build for caching)
RUN ./gradlew :server:dependencies --no-daemon --build-cache --configure-on-demand

# Stage 2: Build the application
FROM gradle:8.10.2-jdk17-alpine AS build
WORKDIR /app

# Copy the cached dependencies
COPY --from=cache /home/gradle/cache_home /home/gradle/.gradle

# Copy the build files again (small and quick)
COPY gradle/ gradle/
COPY gradle.properties settings.gradle.kts build.gradle.kts ./
COPY gradlew ./
COPY gradlew.bat ./

# Copy only the necessary modules (server)
COPY server/ server/

# Build the server application with optimized flags
RUN ./gradlew :server:build --no-daemon --parallel --build-cache --configure-on-demand

# Stage 3: Create a lightweight runtime image
FROM eclipse-temurin:17-jre-alpine

# Set working directory
WORKDIR /app

# Copy the built application from the build stage (using the fat JAR with all dependencies)
COPY --from=build /app/server/build/libs/server-all.jar /app/server.jar

# Expose both local and deployed ports
EXPOSE 4000 8080

# Set environment variable for port based on deployment environment
# For render.com, the PORT environment variable will be set to 8080
# For local development, we'll use the default of 4000
ENV PORT=${PORT:-4000}

# Set the command to run the application with optimized JVM flags
# -XX:+UseContainerSupport: Better container awareness
# -XX:MaxRAMPercentage=75.0: Use up to 75% of available RAM
# -XX:InitialRAMPercentage=50.0: Start with 50% of available RAM
# -XX:+UseG1GC: Use G1 garbage collector (better for services)
# -XX:+AlwaysPreTouch: Touch memory pages on startup for better initial performance
# -XX:+DisableExplicitGC: Prevent explicit GC calls
# -Djava.security.egd=file:/dev/./urandom: Faster entropy source for random numbers
# -Duser.timezone=UTC: Set timezone to UTC
CMD ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-XX:InitialRAMPercentage=50.0", "-XX:+UseG1GC", "-XX:+AlwaysPreTouch", "-XX:+DisableExplicitGC", "-Djava.security.egd=file:/dev/./urandom", "-Duser.timezone=UTC", "-jar", "/app/server.jar"]
