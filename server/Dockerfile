# Multi-stage build for optimized Docker image

# Stage 1: Dependency cache
FROM gradle:8.11.1-jdk17-alpine AS deps
WORKDIR /app
ENV GRADLE_USER_HOME /cache
COPY gradle gradle
COPY gradle.properties settings.gradle.kts build.gradle.kts ./
COPY server server
COPY shared shared
COPY gradle/libs.versions.toml gradle/
# Download dependencies only
RUN gradle :server:dependencies --no-daemon --build-cache

# Stage 2: Build the application
FROM gradle:8.11.1-jdk17-alpine AS build
WORKDIR /app
COPY --from=deps /cache /home/gradle/.gradle
COPY . .
# Build the application with optimized settings
RUN gradle :server:build -x test --no-daemon --parallel --build-cache --configure-on-demand

# Stage 3: Create a minimal runtime image
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Create a non-root user to run the application
RUN addgroup --system --gid 1001 appgroup && \
    adduser --system --uid 1001 --ingroup appgroup appuser && \
    mkdir -p /app/logs && \
    chown -R appuser:appgroup /app

# Copy the built application
COPY --from=build --chown=appuser:appgroup /app/server/build/libs/server-all.jar /app/server.jar

# Set environment variables
ENV PORT=4000
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:InitialRAMPercentage=50.0 -Djava.security.egd=file:/dev/./urandom"

# Expose both ports (4000 for local, 8080 for render.com)
EXPOSE 4000
EXPOSE 8080

# Switch to non-root user
USER appuser

# Run the application
CMD java $JAVA_OPTS -jar /app/server.jar
