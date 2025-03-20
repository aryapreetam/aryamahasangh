# Stage 1: Build the application
FROM gradle:8.10.2-jdk17-alpine AS build
WORKDIR /app

# Copy the entire project
COPY . .

# Build the server application
RUN gradle :server:build --no-daemon

# Stage 2: Create a lightweight runtime image
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

# Set the command to run the application
CMD ["java", "-jar", "/app/server.jar"]