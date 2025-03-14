# Server Module Docker Deployment

This directory contains the server module of the AryaMahasangh project, which is a Ktor-based GraphQL server.

## Docker Setup

A Dockerfile is provided to containerize the server application. The Dockerfile uses a multi-stage build approach to create a lightweight image:
- First stage: Caches Gradle dependencies for faster builds
- Second stage: Builds the application using the cached dependencies
- Third stage: Creates a minimal runtime image with just the built application

## Building and Running Locally

To build and run the Docker image locally:

```bash
# Navigate to the project root directory
cd /path/to/AryaMahasangh

# Build the Docker image
docker build -t aryamahasangh-server -f server/Dockerfile .

# Run the Docker container locally (using port 4000)
docker run -p 4000:4000 aryamahasangh-server
```

The server will be accessible at http://localhost:4000/graphql

Note: When deployed to render.com, the server will use port 8080 instead

## Deploying to Render.com

To deploy this application to Render.com:

1. Push your code to GitHub
2. Log in to Render.com
3. Create a new Web Service
4. Connect your GitHub repository
5. Configure the service:
   - Build Command: Leave empty (the Dockerfile handles the build)
   - Start Command: Leave empty (the Dockerfile handles this)
   - Select "Docker" as the environment
   - Set the root directory to the repository root
   - Set the Docker file path to `server/Dockerfile`

6. Configure environment variables:
   - Note: Render.com automatically sets the PORT environment variable to 8080 for web services
   - You don't need to manually set the PORT variable as the Dockerfile is configured to use it
7. Set the instance type (recommend starting with a small instance)
8. Click "Create Web Service"

## Additional Configuration for Render.com

- **Health Check Path**: You may want to set a health check path to `/graphql` or `/graphiql`
- **Auto-Deploy**: Enable auto-deploy if you want Render to automatically deploy when you push changes to your repository
- **Custom Domains**: Configure a custom domain if needed

## Notes

- The server listens on port 4000 when running locally and port 8080 when deployed to environments like render.com
- The Docker image is based on Alpine Linux for minimal size
- The application is packaged as a self-contained JAR file
- The port is configured using the PORT environment variable, which defaults to 4000 if not set

## Build Optimizations

The Dockerfile has been optimized to reduce build time on render.com from approximately 10 minutes to around 1 minute. Key optimizations include:

- Three-stage build process with dedicated dependency caching
- Selective file copying (only necessary files and modules)
- Leveraging Docker layer caching
- Optimized Gradle build flags (parallel, build-cache, configure-on-demand)
- JVM runtime optimizations for faster startup and better performance
- Removal of shared module dependency
- Added .dockerignore file to reduce build context size

For detailed information about these optimizations, see [DOCKER_OPTIMIZATION.md](./DOCKER_OPTIMIZATION.md)
